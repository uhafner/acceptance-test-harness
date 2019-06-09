#!/bin/bash

display=":42"
vncserver -kill $display > /dev/null
vncserver $display -geometry 1750x1250 -localhost=no 1>&2
if command -v vncviewer >/dev/null 2>&1; then
  vncviewer localhost$display > /dev/null &
fi

