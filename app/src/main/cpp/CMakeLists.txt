cmake_minimum_required(VERSION 3.6)
project(grimald)

if(${ANDROID})
else()
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin")
endif()

set(CMAKE_CXX_STANDARD 11)

add_compile_options(-O3)

if(${ANDROID})
    set(EXE_NAME libgrimald.so)
else()
    set(EXE_NAME grimald)
endif()

add_executable(${EXE_NAME} blake2b-ref.c main.cpp)
