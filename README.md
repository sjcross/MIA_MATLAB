[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4555867.svg)](https://doi.org/10.5281/zenodo.4555867)

About MIA_MATLAB
------------------
MIA_MATLAB is a [MIA](https://github.com/sjcross/MIA) plugin, which adds additional modules that rely on MATLAB.  The latest release includes the following modules:
- "Fit alpha shape" will create an [alpha shape](https://uk.mathworks.com/help/matlab/ref/alphashape.html) for each object in a specified object collection.
- "Sort stack" will order slices of an image stack such that the difference in normalised cross-correlation between adjacent slices is minimised.

Installation
------------
1. Download the version of MIA_MATLAB, corresponding to your copy of MIA, from the [Releases](https://github.com/sjcross/MIA_MATLAB/releases) page.
2. Place this .jar file into the /plugins directory of the your Fiji installation
3. Install the relevant version of MATLAB (or the [MATLAB Compiler Runtime](https://uk.mathworks.com/products/compiler/matlab-runtime.html)).  Filenames for MIA_MATLAB indicate the version of MATLAB to use (e.g. 2018b).
4. Copy the javabuilder.jar file from the installed MATLAB directory (e.g. C:\Program Files\MATLAB\2018b\toolbox\javabuilder\jar) to your Fiji's /jars directory.

Usage
-----
MIA_MATLAB will add various modules to MIA (e.g. the ability to create alpha surfaces for objects).  These can be accessed in the same way as other modules.

Acknowledgements
----------------
This plugin relies on the [MATLAB Compiler Runtime](https://uk.mathworks.com/products/compiler/matlab-runtime.html) or a full copy of MATLAB if available.

Note
----
This plugin is still in development and test coverage is currently incomplete.  Please keep an eye on results and add an [issue](https://github.com/SJCross/MIA_MATLAB/issues) if any problems are encountered.
