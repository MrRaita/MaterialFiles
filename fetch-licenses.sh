#!/usr/bin/env bash
# Resmi lisans metinlerini upstream depolardan indirir (repo kökünde çalıştır).
# Başarısız olan indirmeyi sonda listeler; tekrar çalıştırmak güvenlidir.
cd "$(dirname "$0")" || exit 1
FAIL=0
get() { # url hedef
  mkdir -p "$(dirname "$2")"
  if curl -fsSL --retry 2 -o "$2.tmp" "$1" && [ -s "$2.tmp" ]; then
    mv "$2.tmp" "$2"; echo "OK    $2"
  else
    rm -f "$2.tmp"; echo "HATA  $2  <-  $1"; FAIL=1
  fi
}
F=app/src/main/assets/fonts/licenses
get https://raw.githubusercontent.com/JetBrains/JetBrainsMono/master/OFL.txt          $F/OFL-JetBrainsMono.txt
get https://raw.githubusercontent.com/tonsky/FiraCode/master/LICENSE                  $F/OFL-FiraCode.txt
get https://raw.githubusercontent.com/adobe-fonts/source-code-pro/release/LICENSE.md  $F/OFL-SourceCodePro.txt
get https://raw.githubusercontent.com/microsoft/cascadia-code/main/LICENSE            $F/OFL-CascadiaCode.txt
T=app/src/main/assets/textmate/LICENSES
get https://raw.githubusercontent.com/atom/language-java/master/LICENSE.md                       $T/language-java-MIT.txt
get https://raw.githubusercontent.com/microsoft/TypeScript-TmLanguage/master/LICENSE.txt         $T/TypeScript-TmLanguage-MIT.txt
get https://raw.githubusercontent.com/MagicStack/MagicPython/master/LICENSE                      $T/MagicPython-MIT.txt
get https://raw.githubusercontent.com/microsoft/vscode-markdown-tm-grammar/main/LICENSE.txt      $T/vscode-markdown-tm-grammar-MIT.txt
get https://raw.githubusercontent.com/sumneko/lua.tmbundle/master/LICENSE                        $T/lua.tmbundle-MIT.txt
get https://raw.githubusercontent.com/zhanghai/AndroidRetroFile/master/LICENSE                   LICENSES/AndroidRetroFile-GPL-2.0-with-Classpath-Exception.txt
get https://raw.githubusercontent.com/eclipse-tm4e/tm4e/main/LICENSE                              LICENSES/EPL-2.0.txt
echo
[ $FAIL = 0 ] && echo "Hepsi indirildi." || echo "Bazı dosyalar inmedi; ağ/URL kontrol edip tekrar çalıştır."
exit $FAIL
