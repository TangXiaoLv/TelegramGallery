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

#LOCAL_CFLAGS 	:=
#LOCAL_CFLAGS 	+=
#LOCAL_CFLAGS 	+=
#LOCAL_CPPFLAGS 	:=
#LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -latomic

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -w -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DBSD=1 -ffast-math -Os -funroll-loops")

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