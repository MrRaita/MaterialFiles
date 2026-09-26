package me.zhanghai.android.files.viewer.image.edit
import me.zhanghai.android.files.provider.common.deleteIfExists

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.github.chrisbanes.photoview.PhotoView
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.R
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.file.fileProviderUri
import com.yalantis.ucrop.UCrop
import me.zhanghai.android.files.filelist.FileListActivity
import me.zhanghai.android.files.filejob.FileJobService
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.extraPathList
import me.zhanghai.android.files.app.AppActivity

class ImageEditorActivity : AppActivity() {
    private lateinit var path: Path
    private lateinit var image: PhotoView
    private var bitmap: Bitmap? = null
    private var changed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_editor)
        path = intent.extraPath ?: run { finish(); return }
        image = findViewById(R.id.image)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = path.fileName.toString()
        try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(path.fileProviderUri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 2048, 2048)
            }
            bitmap = contentResolver.openInputStream(path.fileProviderUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: run {
                finish()
                return
            }
            image.setImageBitmap(bitmap)
        } catch (e: Exception) { finish() }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        if (width <= 0 || height <= 0) return 1

        var sampleSize = 1
        while (
            width / sampleSize > reqWidth ||
            height / sampleSize > reqHeight
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.image_editor, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_crop -> { startCrop(); true }
        R.id.action_rotate_left -> { rotate(-90f); true }
        R.id.action_rotate_right -> { rotate(90f); true }
        R.id.action_flip_horizontal -> { image.scaleX = -image.scaleX; changed = true; true }
        R.id.action_flip_vertical -> { image.scaleY = -image.scaleY; changed = true; true }
        R.id.action_grayscale -> { image.colorFilter = if (image.colorFilter == null) ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) else null; changed = true; true }
        R.id.action_save_copy -> { saveCopy(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun rotate(degrees: Float) { image.rotation += degrees; changed = true }

    private fun startCrop() {
        try {
            val dir = Paths.get(cacheDir.path, "ucrop").also { it.toFile().mkdirs() }
            val output = dir.resolve("${path.fileName.toString().substringBeforeLast('.')}_crop_${System.currentTimeMillis()}.png")
            val options = UCrop.Options().apply {
                setToolbarTitle(getString(R.string.image_editor_crop))
                setFreeStyleCropEnabled(true)
                setHideBottomControls(false)
            }
            startActivityForResult(UCrop.of(path.fileProviderUri, output.fileProviderUri).withOptions(options).getIntent(this), REQUEST_CROP)
            pendingCropPath = output
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun saveCopy() {
        val source = bitmap ?: return
        val matrix = android.graphics.Matrix().apply { postRotate(image.rotation); postScale(image.scaleX, image.scaleY) }
        var transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (image.colorFilter != null) {
            val filtered = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
            Canvas(filtered).drawBitmap(transformed, 0f, 0f, Paint().apply { colorFilter = image.colorFilter })
            transformed.recycle()
            transformed = filtered
        }
        val output = java.io.ByteArrayOutputStream()
        transformed.compress(Bitmap.CompressFormat.PNG, 100, output)
        transformed.recycle()
        val intent = FileListActivity.CreateFileContract().createIntent(this, Triple(MimeType.IMAGE_ANY, path.fileName.toString().substringBeforeLast('.') + "_edited.png", null))
        startActivityForResult(intent, 9901)
        pendingBytes = output.toByteArray()
    }

    private var pendingBytes: ByteArray? = null
    private var pendingCropPath: Path? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                val output = pendingCropPath
                if (output != null) {
                    val intent = FileListActivity.CreateFileContract().createIntent(
                        this, Triple(MimeType.IMAGE_ANY, path.fileName.toString().substringBeforeLast('.') + "_crop.png", null)
                    )
                    startActivityForResult(intent, REQUEST_SAVE_CROP)
                }
            } else {
                val error = data?.let { UCrop.getError(it) }
                error?.printStackTrace()
                pendingCropPath = null
            }
            return
        }
        if (requestCode == REQUEST_SAVE_CROP && resultCode == RESULT_OK) {
            val target = data?.extraPath
            val output = pendingCropPath
            if (target != null && output != null) {
                contentResolver.openInputStream(output.fileProviderUri)?.use { input ->
                    FileJobService.write(target, input.readBytes(), this) { }
                }
            }
            output?.let { try { it.deleteIfExists() } catch (_: Exception) {} }
            pendingCropPath = null
            return
        }
        if (requestCode == 9901 && resultCode == RESULT_OK) {
            val target = data?.extraPath
            pendingBytes?.let { bytes -> if (target != null) FileJobService.write(target, bytes, this) { } }
            pendingBytes = null
        }
    }

    override fun onDestroy() { bitmap?.recycle(); super.onDestroy() }

    companion object {
        private const val REQUEST_CROP = 9902
        private const val REQUEST_SAVE_CROP = 9903
    }
}
