Pixylator plugin
=================

Pixylator is a color-tracking plugin for [ImageJ](https://imagej.nih.gov/ij/). It uses Video I/O, which in turn engages [JavaCPP](https://github.com/bytedeco/javacpp) and [JavaCPP-presets](https://github.com/bytedeco/javacpp-presets) to access features of [FFmpeg](https://ffmpeg.org/).

Although it claims to be in the beta phase, it is not tested so much in different ImageJ environments. __Please let me know of any installation problems or weird behaviors__.


How to use
-----------

1. Download [ImageJ](https://imagej.nih.gov/ij/) (Java>=1.6) if you do not have one. I recommend using 'ImageJ1' rather than ['ImageJ2'(aka 'Fiji')](http://imagej.net/Welcome), because I test primarily using the former.
2. ~~download [FFmpeg](https://ffmpeg.org/).~~ _Update: it looks like it is not necessary to download FFmpeg, because the jar file(s) already contain the DLLs (although I have not yet tried without a FFmpeg installation). Just skip this step._
3. download and extract 'build/Pixylator_build.tar.gz' from this repository.
4. copy the resulting 'Pixylator' directory into ImageJ's "plugin" directory.
5. (re)open ImageJ. "Pixylator" menu will show up in the Plugins menu.


Building from source
---------------------

Because of various possible incompatibilities, the binary may not work out of the box. Or you may not be satisfied with the internal algorithms.

In those cases, just run `make` in this repository.

Note that the configuration is just for my environment (Mac, with a JRE-bundled ImageJ), so you may have to change the following variables:

+ 'RT': the path to 'rt.jar' in the Java SE runtime library (i.e. the one containing class files for 'String', 'System' etc.).
+ 'IJ': the path to 'ij.jar' contained in ImageJ executable (this is the actual Java executable for ImageJ). In most cases it is in the same directory as 'ImageJ.exe'.

Typically, you can run `make plugin` from inside the ImageJ's plugin directory, with options such as below:

```
$ make -DRT=path/to/your/rt.jar -DIJ=path/to/your/ij.jar plugin
```

It is important that __you specify jar files (rt.jar and ij.jar) which ImageJ actually runs with__. Otherwise Pixylator plugin will not show up, or it fails to run.


Troubleshooting
----------------

### 'Pixylator' submenu does not show up

1. Make sure that all the other plugins are available (otherwise it is a general problem that ImageJ does not recognize the plugin directory).
2. It is likely that __your ImageJ does not recognize Pixylator plugin__. Usually the problem is solved by re-compiling it.
    + Clone or download this repository, and replace the 'Pixylator' plugin directory with it.
    + If you are using [ImageJ (aka ImageJ)](https://imagej.nih.gov/ij/): use 'Plugins'>'Compile and Run...', and select 'plugins/Pixylator/Pixylator_beta.java'.
    + If you are using [ImageJ2 or Fiji](http://imagej.net/Welcome), or if the former method did not work for you, try building from the source (see the section above).

Whenever you re-compile Pixylator plugin, it would be better re-starting ImageJ to avoid any version-difference issues.


### Pixylator plugin hangs from time to time

There may be some memory-related errors in the FFmpeg library (although I hope it is unlikely...). Try increasing ImageJ's memory to e.g. 10 GB (from 'Edit'>'Options'>'Memory & Threads'...), and restart it.

If the problem persists, it would be nice that you could notify me!


### There are weird errors when ran in ImageJ2/Fiji

Because Pixylator takes advantages of being a (legacy) ImageJ1 plugin, running it from ImageJ2 can be problematic. Unfortunately, there are only one thing that you can do currently:

1. Rebuild Pixylator plugin from the source (see above), to avoid any Java version incompatibility-derived issues.

If the problem persists, __it may be because of the difference in the way ImageJ1 or ImageJ2 loads its plugins__. I would appreciate any information on what occurs (or, more helpfully, how to get away with it;-) ).
