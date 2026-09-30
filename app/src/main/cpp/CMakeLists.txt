cmake_minimum_required(VERSION 3.22)
project(saarthi_voice LANGUAGES C CXX)

# Debug APKs are the only car-test artifact; keep the native inference optimized.
add_compile_options(-O3 -DNDEBUG)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp whisper-build)

add_library(saarthi_voice SHARED voice_jni.cpp)
target_link_libraries(saarthi_voice PRIVATE whisper log)
