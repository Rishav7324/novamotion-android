cmake_minimum_required(VERSION 3.22.1)

project("novamotion" CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable aggressive optimization for release builds
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG -ffast-math -fvisibility=hidden")

# Source files
set(SOURCES
    native_bridge.cpp
    spline_interpolator.cpp
    fast_optical_flow.cpp
    audio_dsp.cpp
    color_lut.cpp
)

add_library(
    novamotion
    SHARED
    ${SOURCES}
)

# Link standard Android native libraries
target_link_libraries(
    novamotion
    android
    log
    m
)
