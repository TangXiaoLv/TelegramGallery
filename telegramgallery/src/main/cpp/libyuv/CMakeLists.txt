cmake_minimum_required(VERSION 3.4.1)
project(yuv C CXX)

aux_source_directory(${LIBYUV}/source SOURCE_YUV)

INCLUDE_DIRECTORIES(${LIBYUV}/include)
INCLUDE_DIRECTORIES(${LIBYUV}/include/libyuv)
INCLUDE_DIRECTORIES(${LIBYUV}/mbedtls)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -Wl")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -Wl")

add_library(yuv STATIC ${SOURCE_YUV})