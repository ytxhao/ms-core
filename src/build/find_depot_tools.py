#!/usr/bin/env python
# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
"""Small utility function to find depot_tools and add it to the python path.

Will throw an ImportError exception if depot_tools can't be found since it
imports breakpad.

This can also be used as a standalone script to print out the depot_tools
directory location.
"""

from __future__ import print_function

import os
import sys


# Path to //src
SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))

print("yuhaooooooooooo SRC:"+SRC)
def IsRealDepotTools(path):
  expanded_path = os.path.expanduser(path)
  return os.path.isfile(os.path.join(expanded_path, 'gclient.py'))


def add_depot_tools_to_path():
  """Search for depot_tools and add it to sys.path."""
  # First, check if we have a DEPS'd in "depot_tools".
  deps_depot_tools = os.path.join(SRC, 'third_party', 'depot_tools')
  sys.path.insert(0, deps_depot_tools)
  return deps_depot_tools

DEPOT_TOOLS_PATH = add_depot_tools_to_path()

# pylint: disable=W0611
# import breakpad


def main():
  if DEPOT_TOOLS_PATH is None:
    return 1
  print(DEPOT_TOOLS_PATH)
  return 0


if __name__ == '__main__':
  sys.exit(main())
