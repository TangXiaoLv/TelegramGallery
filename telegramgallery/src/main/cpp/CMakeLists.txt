cmake_minimum_required(VERSION 3.4.1)
project(gly C CXX)
set(LIBYUV ${CMAKE_CURRENT_SOURCE_DIR}/libyuv)
set(LIBAVFORMAT ${CMAKE_CURRENT_SOURCE_DIR}/libavformat)
add_subdirectory(libyuv)

if (${CMAKE_ANDROID_ARCH_ABI} STREQUAL "armeabi-v7a")
    set(LIB_STATIC ${LIBAVFORMAT}/armeabi-v7a)
elseif(${CMAKE_ANDROID_ARCH_ABI} STREQUAL "armeabi")
    set(LIB_STATIC ${LIBAVFORMAT}/armeabi)
elseif(${CMAKE_ANDROID_ARCH_ABI} STREQUAL "x86")
    set(LIB_STATIC ${LIBAVFORMAT}/x86)
endif ()

INCLUDE_DIRECTORIES(${LIBYUV}/include)
INCLUDE_DIRECTORIES(${LIBAVFORMAT}/include)

aux_source_directory(. SOURCE)

add_definitions("-fPIC")
#set(CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS "${CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS} -undefined dynamic_lookup")

#link_directories(${LIB_STATIC})

find_library(LIB_JNIGRAPHICS jnigraphics)
set(LIB_AVCODEC ${LIB_STATIC}/libavcodec.a)
set(LIB_AVFORMAT ${LIB_STATIC}/libavformat.a)
set(LIB_AVUTIL ${LIB_STATIC}/libavutil.a)

message("${LIB_STATIC}")
message("${LIB_ANDROID}")
message("${LIB_AVCODEC}")

add_library(gly SHARED ${SOURCE})

#target_link_libraries(${LIB_AVFORMAT} ${LIB_AVUTIL} ${LIB_AVCODEC})
target_link_libraries(gly ${LIB_JNIGRAPHICS} ${LIB_AVUTIL} ${LIB_AVCODEC} ${LIB_AVFORMAT} yuv)

#${liblog}
#${LIB_AVCODEC}