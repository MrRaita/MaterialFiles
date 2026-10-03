/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.image

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.ImageViewerFragmentBinding
import me.zhanghai.android.files.file.asFileSize
import me.zhanghai.android.files.file.formatShort
import me.zhanghai.android.files.file.fileProviderUri
import me.zhanghai.android.files.provider.common.delete
import me.zhanghai.android.files.provider.common.readAttributes
import me.zhanghai.android.files.ui.DepthPageTransformer
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.ParcelableListParceler
import me.zhanghai.android.files.util.ParcelableState
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.createSendImageIntent
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.extraPathList
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.getState
import me.zhanghai.android.files.util.mediumAnimTime
import me.zhanghai.android.files.util.putState
import me.zhanghai.android.files.util.showToast
import me.zhanghai.android.files.util.startActivitySafe
import me.zhanghai.android.files.util.withChooser
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.IOException

// Google Photos style viewer: title-only top bar, Share/Delete at the bottom, and swiping up on
// the image reveals a bottom-sheet info panel (name, size, date, path). No overflow menu.
class ImageViewerFragment : Fragment(), ConfirmDeleteDialogFragment.Listener {
    private val args by args<Args>()
    private val argsPaths by lazy { args.intent.extraPathList }

    private lateinit var paths: MutableList<Path>

    private lateinit var binding: ImageViewerFragmentBinding

    private lateinit var systemUiHelper: SystemUiHelper

    private lateinit var adapter: ImageViewerAdapter

    private lateinit var infoPanelBehavior: BottomSheetBehavior<androidx.core.widget.NestedScrollView>

    private lateinit var swipeDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paths = (savedInstanceState?.getState<State>()?.paths ?: argsPaths).toMutableList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ImageViewerFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (paths.isEmpty()) {
            // TODO: Show a toast.
            finish()
            return
        }

        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Our app bar will draw the status bar background.
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
        binding.bottomBar.applySystemWindowInsetsToPadding(left = true, bottom = true, right = true)
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        ) { visible: Boolean ->
            binding.appBarLayout.animate()
                .alpha(if (visible) 1f else 0f)
                .translationY(if (visible) 0f else -binding.appBarLayout.bottom.toFloat())
                .setDuration(mediumAnimTime.toLong())
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
            binding.bottomBar.animate()
                .alpha(if (visible) 1f else 0f)
                .translationY(if (visible) 0f else binding.bottomBar.height.toFloat())
                .setDuration(mediumAnimTime.toLong())
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
        // This will set up window flags.
        systemUiHelper.show()
        adapter = ImageViewerAdapter(viewLifecycleOwner) { systemUiHelper.toggle() }.apply {
            replace(paths)
        }
        binding.viewPager.apply {
            // 1 is the default for the old androidx.viewpager.widget.ViewPager.
            offscreenPageLimit = 1
            adapter = this@ImageViewerFragment.adapter
            // ViewPager saves its position and will restore it later.
            setCurrentItem(args.position, false)
            setPageTransformer(DepthPageTransformer)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTitle()
                    updateInfoPanel()
                }
            })
        }
        binding.shareButton.setOnClickListener { share() }
        binding.deleteButton.setOnClickListener { confirmDelete() }

        infoPanelBehavior = BottomSheetBehavior.from(binding.infoPanel).apply {
            isHideable = true
            peekHeight = 0
            state = BottomSheetBehavior.STATE_HIDDEN
        }
        swipeDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val deltaY = e2.y - e1.y
                    return if (deltaY < -100 && velocityY < -600) {
                        infoPanelBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        true
                    } else if (deltaY > 100 && velocityY > 600) {
                        infoPanelBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        true
                    } else {
                        false
                    }
                }
            }
        )
        // Never consume the touch: let the ViewPager/PhotoView keep handling paging, panning and
        // zooming as usual. We only ever look at the gesture to detect a vertical swipe fling.
        binding.viewPager.getChildAt(0)?.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (paths.isEmpty()) {
            // We did finish the activity in onActivityCreated(), however we will still be called
            // here before the activity is actually finished.
            return
        }

        updateTitle()
        updateInfoPanel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putState(State(paths))
    }

    private fun confirmDelete() {
        ConfirmDeleteDialogFragment.show(currentPath, this)
    }

    override fun delete(path: Path) {
        try {
            path.delete()
        } catch (e: IOException) {
            e.printStackTrace()
            showToast(e.toString())
            return
        }
        paths.removeAll(listOf(path))
        if (paths.isEmpty()) {
            finish()
            return
        }
        adapter.replace(paths)
        // ViewPager only asynchronously sets current item to 0, which isn't a desirable behavior
        // for us and will make updateTitle() crash for index out of bounds.
        if (binding.viewPager.currentItem > paths.lastIndex) {
            binding.viewPager.currentItem = paths.lastIndex
        }
        updateTitle()
        updateInfoPanel()
        // Work around blank screen due to ViewPager2.PageTransformer not being called (and thus the
        // next item keeps its 0 alpha) when we have offscreenPageLimit = 1.
        binding.viewPager.doOnPreDraw { binding.viewPager.requestTransform() }
    }

    private fun updateTitle() {
        requireActivity().title = currentPath.fileName.toString()
    }

    private fun updateInfoPanel() {
        infoPanelBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        val path = currentPath
        val context = requireContext()
        binding.infoName.text = path.fileName.toString()
        try {
            val attributes = path.readAttributes(BasicFileAttributes::class.java)
            val size = attributes.size().asFileSize().formatHumanReadable(context)
            val date = attributes.lastModifiedTime().toInstant().formatShort(context)
            val dimensions = runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(path.fileProviderUri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                if (options.outWidth > 0 && options.outHeight > 0) {
                    "${options.outWidth} × ${options.outHeight}"
                } else {
                    null
                }
            }.getOrNull()
            binding.infoDetails.text = listOfNotNull(dimensions, size, date).joinToString("  ·  ")
        } catch (e: Exception) {
            e.printStackTrace()
            binding.infoDetails.text = null
        }
        binding.infoPath.text = path.toString()
    }

    private fun share() {
        val path = currentPath
        val intent = path.fileProviderUri.createSendImageIntent()
            .apply { extraPath = path }
            .withChooser()
        startActivitySafe(intent)
    }

    private val currentPath: Path
        get() = paths[binding.viewPager.currentItem]

    @Parcelize
    class Args(val intent: Intent, val position: Int) : ParcelableArgs

    @Parcelize
    private class State(val paths: @WriteWith<ParcelableListParceler> List<Path>) : ParcelableState
}
