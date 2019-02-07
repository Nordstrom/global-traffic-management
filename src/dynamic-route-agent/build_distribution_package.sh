#!/bin/bash
pex . --python=python3 -v -e dra.dynamicrouteagent:main -o dist/dynamicrouteagent.pex --disable-cache
