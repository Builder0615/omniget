#!/usr/bin/env bash
set -euo pipefail

WEBP_VERSION="${WEBP_VERSION:-v1.6.0}"
ABIS=(arm64-v8a x86_64)
LIBS=(sharpyuv webp webpdecoder webpdemux webpmux)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$ANDROID_DIR/app/src/main/assets/ffmpeg16k"

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
    ANDROID_NDK_HOME="${ANDROID_NDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/ndk/27.0.12077973}"
fi

NDK_TOOLCHAIN="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"
LLVM_PREBUILT="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt"
if [[ ! -f "$NDK_TOOLCHAIN" || ! -d "$LLVM_PREBUILT" ]]; then
    echo "Android NDK 27 was not found. Set ANDROID_NDK_HOME." >&2
    exit 1
fi

HOST_TAG="$(find "$LLVM_PREBUILT" -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | head -n 1)"
LLVM_BIN="$LLVM_PREBUILT/$HOST_TAG/bin"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/omniget-webp.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

git clone --depth 1 --branch "$WEBP_VERSION" \
    https://chromium.googlesource.com/webm/libwebp "$WORK_DIR/libwebp"

for abi in "${ABIS[@]}"; do
    build_dir="$WORK_DIR/build-$abi"
    cmake -S "$WORK_DIR/libwebp" -B "$build_dir" -G Ninja \
        -DCMAKE_TOOLCHAIN_FILE="$NDK_TOOLCHAIN" \
        -DANDROID_ABI="$abi" \
        -DANDROID_PLATFORM=android-26 \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
        -DBUILD_SHARED_LIBS=ON \
        -DWEBP_BUILD_CWEBP=OFF \
        -DWEBP_BUILD_DWEBP=OFF \
        -DWEBP_BUILD_GIF2WEBP=OFF \
        -DWEBP_BUILD_IMG2WEBP=OFF \
        -DWEBP_BUILD_VWEBP=OFF \
        -DWEBP_BUILD_WEBPINFO=OFF \
        -DWEBP_BUILD_WEBPMUX=OFF \
        -DWEBP_BUILD_ANIM_UTILS=OFF \
        -DWEBP_BUILD_EXTRAS=OFF \
        -DWEBP_BUILD_LIBWEBPMUX=ON
    cmake --build "$build_dir" --parallel

    mkdir -p "$OUTPUT_DIR/$abi"
    for lib in "${LIBS[@]}"; do
        source_file="$build_dir/lib$lib.so"
        "$LLVM_BIN/llvm-strip" --strip-unneeded "$source_file"
        if "$LLVM_BIN/llvm-readelf" -l "$source_file" |
            awk '/LOAD/ && $NF != "0x4000" { exit 1 }'; then
            cp "$source_file" "$OUTPUT_DIR/$abi/lib$lib.so"
        else
            echo "$abi/lib$lib.so is not 16 KB aligned" >&2
            exit 1
        fi
    done
done

cp "$WORK_DIR/libwebp/COPYING" "$OUTPUT_DIR/LICENSE.webp"
echo "Updated 16 KB WebP assets in $OUTPUT_DIR"
