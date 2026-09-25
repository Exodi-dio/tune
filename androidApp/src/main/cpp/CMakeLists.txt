cmake_minimum_required(VERSION 3.22.1)
project(tune_player LANGUAGES C CXX)

set(FFMPEG_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(FFMPEG_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/../../../build/ffmpeg/include")

if(NOT EXISTS "${FFMPEG_INCLUDE}/libavcodec/avcodec.h")
    message(FATAL_ERROR "FFmpeg artifacts are missing. Run: bash scripts/build-ffmpeg-android.sh ${ANDROID_ABI}")
endif()

foreach(library avutil swresample avcodec avformat)
    add_library(${library} SHARED IMPORTED)
    set_target_properties(${library} PROPERTIES IMPORTED_LOCATION "${FFMPEG_ROOT}/lib${library}.so")
endforeach()

add_library(tune_player SHARED ffmpeg_player.cpp)
target_include_directories(tune_player PRIVATE "${FFMPEG_INCLUDE}")
target_compile_options(tune_player PRIVATE -Oz -ffunction-sections -fdata-sections)
target_link_options(tune_player PRIVATE -Wl,--gc-sections -Wl,-z,max-page-size=16384)
target_link_libraries(tune_player avformat avcodec swresample avutil aaudio android log)
