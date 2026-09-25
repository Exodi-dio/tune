#!/usr/bin/env bash
# Builds the Android FFmpeg decoder used by Tune's androidApp.
# Adapted from the upstream mobile FFmpeg build script (GPLv3):
# repo root IS the mobile project (no mobile/ subdir), SDK auto-detected,
# portable across Linux and macOS runners/hosts.
#
# Source is downloaded to a temporary cache exactly like the upstream script;
# no FFmpeg source or generated library is committed to this repository.
#
# Usage: bash scripts/build-ffmpeg-android.sh [arm64-v8a]
set -euo pipefail

FFMPEG_VERSION="8.1.2"
FFMPEG_URL="https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.gz"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANDROID_DIR="${REPO_ROOT}/androidApp"
NDK_VERSION="30.0.15729638"
API=31

detect_sdk_dir() {
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then echo "${ANDROID_SDK_ROOT}"; return; fi
    if [[ -n "${ANDROID_HOME:-}" ]]; then echo "${ANDROID_HOME}"; return; fi
    if [[ -f "${REPO_ROOT}/local.properties" ]]; then
        awk -F= '/^sdk.dir=/{print $2}' "${REPO_ROOT}/local.properties"
        return
    fi
    echo ""
}
SDK_DIR="$(detect_sdk_dir)"
[[ -n "${SDK_DIR}" ]] || { echo "Android SDK not found (set ANDROID_SDK_ROOT or sdk.dir in local.properties)" >&2; exit 1; }
NDK="${SDK_DIR}/ndk/${NDK_VERSION}"

if [[ -d "${NDK}/toolchains/llvm/prebuilt/linux-x86_64" ]]; then
    HOST_TAG="linux-x86_64"
else
    DARWIN_CANDIDATE="$(echo "${NDK}"/toolchains/llvm/prebuilt/darwin-*)"
    if [[ -d "${DARWIN_CANDIDATE%% *}" ]]; then
        HOST_TAG="$(basename "${DARWIN_CANDIDATE%% *}")"
    else
        echo "No NDK LLVM prebuilt toolchain found under ${NDK}" >&2; exit 1
    fi
fi
TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/${HOST_TAG}"
BUILD_DIR="${TMPDIR:-/tmp}/ffmpeg-build-tune-android-${FFMPEG_VERSION}"
JNI_OUT="${ANDROID_DIR}/src/main/jniLibs"
INCLUDE_OUT="${ANDROID_DIR}/build/ffmpeg/include"

[[ -x "${TOOLCHAIN}/bin/clang" ]] || { echo "Android NDK ${NDK_VERSION} is required at ${NDK}" >&2; exit 1; }

# FFmpeg is the sole decoder. Enable its complete decoder, demuxer and parser
# registry so every music format supported by the pinned upstream source stays
# supported without maintaining a fragile allow-list. Programs, encoders,
# muxers, filters, devices and network protocols remain excluded.

NPROC="$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)"

download_source() {
    mkdir -p "${BUILD_DIR}/src"
    if [[ ! -f "${BUILD_DIR}/ffmpeg.tar.gz" ]]; then
        echo "==> Downloading FFmpeg ${FFMPEG_VERSION}..."
        curl --fail --location --retry 3 "${FFMPEG_URL}" -o "${BUILD_DIR}/ffmpeg.tar.gz"
    fi
}

build_arch() {
    local ABI="$1" ARCH TRIPLE
    case "${ABI}" in
        arm64-v8a) ARCH="aarch64"; TRIPLE="aarch64-linux-android" ;;
        *) echo "Unsupported ABI: ${ABI}" >&2; exit 1 ;;
    esac
    local SRC="${BUILD_DIR}/src-${ABI}" INSTALL="${BUILD_DIR}/install-${ABI}"
    rm -rf "${SRC}" "${INSTALL}"
    mkdir -p "${SRC}" "${INSTALL}"
    tar -xzf "${BUILD_DIR}/ffmpeg.tar.gz" -C "${SRC}" --strip-components=1
    echo "==> Building FFmpeg ${FFMPEG_VERSION} for ${ABI}..."
    pushd "${SRC}" >/dev/null
    ./configure \
        --prefix="${INSTALL}" \
        --target-os=android --arch="${ARCH}" --enable-cross-compile \
        --cc="${TOOLCHAIN}/bin/${TRIPLE}${API}-clang" \
        --cxx="${TOOLCHAIN}/bin/${TRIPLE}${API}-clang++" \
        --ar="${TOOLCHAIN}/bin/llvm-ar" --ranlib="${TOOLCHAIN}/bin/llvm-ranlib" --strip="${TOOLCHAIN}/bin/llvm-strip" \
        --disable-everything --disable-autodetect --disable-programs --disable-doc --disable-debug --disable-network \
        --enable-shared --disable-static --enable-small --enable-pic \
        --enable-avutil --enable-avcodec --enable-avformat --enable-swresample \
        --enable-protocol=file --enable-decoders --enable-demuxers --enable-parsers \
        --disable-avdevice --disable-avfilter --disable-swscale \
        --extra-cflags="-Oz -ffunction-sections -fdata-sections" \
        --extra-ldflags="-Wl,--gc-sections -Wl,-z,max-page-size=16384"
    make -j"${NPROC}"
    make install
    popd >/dev/null
    mkdir -p "${JNI_OUT}/${ABI}"
    for lib in libavutil libswresample libavcodec libavformat; do
        cp -L "${INSTALL}/lib/${lib}.so" "${JNI_OUT}/${ABI}/${lib}.so"
    done
    rm -rf "${INCLUDE_OUT}"
    mkdir -p "$(dirname "${INCLUDE_OUT}")"
    cp -R "${INSTALL}/include" "${INCLUDE_OUT}"
}

download_source
case "${1:-arm64-v8a}" in
    arm64-v8a) build_arch arm64-v8a ;;
    *) echo "Usage: $0 [arm64-v8a]" >&2; exit 1 ;;
esac
echo "==> Done. Generated FFmpeg libraries are in ${JNI_OUT}."