# Linux Installation Guide

LNX_Studio is availiable through pre-alpha early access builds on Linux. This means that whilst most things do work, not everything will. You're very welcome to try out LNX on the Linux flavour of your choice but don't blame me if it doesn't work. Basically here be dragons - you have been warned.

The installation process is manual at this stage. For the full release it will all be in a standalone package like the OSX build. Having said that, it should be quite straight forward.

## 1) Prerequisites

LNX_Studio requires the following software to be installed on your machine: `curl ffmpeg xclip`. You can easily install any dependencies on a debian system by running the `linux/install_deps.sh` script.

## 2) SuperCollider and SC3-Plugins

LNX_Studio is built on top of the amazing SuperCollider language and synth engine. You will need working versions of both [SuperCollider](https://github.com/supercollider/supercollider) and [SC3-Plugins](https://github.com/supercollider/sc3-plugins) which are available from their respective GitHub pages. LNX_Studio should work with SuperCollider 3.6+.

## 3) Installation

1. Clone this repository and switch to the `xplat` branch. This is where all cross-platform development is done.
3. Edit the `sclang_conf_lnx.yaml` to point to the correct paths for the following:
    1.  SuperCollider class library
    2.  SC3-Plugins class library
    3.  LNX_Studio class library
4. Dont forget to edit the excludePaths too.
5. Edit the LNX_Studio script to have the correct working directory (`-d` option).
6. Make sure the LNX_Studio script is executable.

## 4) Running LNX

To run LNX, simply open up a terminal, cd to the LNX directory and type `./LNX_Studio`. You can also set up a launcher to do this quite simply and use `lnx.jpg` as the icon.

## 5) Keeping up-to-date

Most of the code changes will be within LNX's SCClassLibrary, so to update to the latest version you will just need to do a `git pull` from your repo directory. If at any point you need to more than this, we'll update these instructions.