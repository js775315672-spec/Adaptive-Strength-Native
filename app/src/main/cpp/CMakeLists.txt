cmake_minimum_required(VERSION 3.22.1)
project(ase_native LANGUAGES CXX)
add_library(ase_native SHARED jni.cpp)
target_compile_features(ase_native PRIVATE cxx_std_17)
