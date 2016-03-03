#!/usr/bin/env bash
dir=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

cd $dir/..

git subtree push --prefix brilleappen git@github.com:itk-google-glass/brilleappen.git develop --squash

cd -
