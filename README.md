SeaDAS Toolbox (seadas-toolbox)
==========================

A toolbox for the OBPG processing code.

[![Build Status](https://travis-ci.org/senbox-org/s3tbx.svg?branch=master)](https://travis-ci.org/senbox-org/s3tbx)
[![Coverity Scan Status](https://scan.coverity.com/projects/7247/badge.svg)](https://scan.coverity.com/projects/senbox-org-s3tbx)

Building seadas-toolbox from the source
------------------------------

Download and install the required build tools
* Install J2SE 1.8 JDK and set JAVA_HOME accordingly.
* Install Maven and set MAVEN_HOME accordingly.
* Install git

Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

Clone the following SeaDAS git repositories into a directory referred to here as [SEADAS]

    cd ${snap}
    git clone https://github.com/seadas/seadas-toolbox.git
    git clone https://github.com/senbox-org/s3tbx.git
    git clone https://github.com/senbox-org/snap-desktop.git
    git clone https://github.com/senbox-org/snap-engine.git

Checkout and build the corresponding branches for your desired release.  See SeaDAS Release Tags (below) for other SeaDAS 8 versions.

SNAP-Engine:

    cd [SEADAS]/snap-engine
    git checkout SEADAS-8.1.0 -b SEADAS-8.1.0-tag
    mvn install -Dmaven.test.skip=true
    *NOTE if mvn fails then try: 'mvn install -Dskiptests=true'

SNAP-Desktop:

    cd [SEADAS]/snap-desktop
    git checkout SEADAS-8.1.0 -b SEADAS-8.1.0-tag
    mvn install -Dmaven.test.skip=true

Sentinel-3 Toolbox:

    cd [SEADAS]/s3tbx
    git checkout SEADAS-8.1.0 -b SEADAS-8.1.0-tag
    mvn install -Dmaven.test.skip=true

SeaDAS Toolbox:

    cd [SEADAS]/seadas-toolbox
    git checkout 1.1.0 -b 1.1.0-tag
    mvn install -Dmaven.test.skip=true



Setting up IntelliJ IDEA
------------------------


1. In IntelliJ IDEA, select "Import Project" and select the ${snap} directory. (Some versions: select "New -> Project From Existing Sources", then navigate upwards in the file selector to select the ${snap} directory, then select "Open")
2. Select "Import project from external model" -> "Maven"
3. Ensure the "Root directory" is ${snap}. (Note: put your actual path).
   Select "Search for projects recursively"; Ensure **not** to enable the option *Create module groups for multi-module Maven projects*. Everything can be default values.

4. Set the used SDK for the main project. A JDK 1.8.0_241 is ideally needed.

5. Use the following configuration to run SNAP in the IDE:

   **Main class:** `org.esa.snap.nbexec.Launcher`

   **VM parameters:** `-Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false`

   All VM parameters are optional

   **Program arguments:**    
   `--userdir "${snap}/seadas-toolbox/target/userdir"`
   `--clusters "${snap}/seadas-toolbox/seadas-kit/target/netbeans_clusters/seadas:${snap}/s3tbx/s3tbx-kit/target/netbeans_clusters/s3tbx"`
   `--patches "${snap}/snap-engine/$/target/classes:${snap}/seadas-toolbox/$/target/classes:${snap}/s3tbx/$/target/classes"`

   **Working directory:** `${snap}/snap-desktop/snap-application/target/snap/`

   **Use classpath of module:** `snap-main`



SeaDAS Release Tags
------------------------
SeaDAS Release: 8.1.0

    seadas-toolbox: 1.1.0		
    s3tbx: SEADAS-8.1.0	
    snap-desktop: SEADAS-8.1.0	
    snap-engine: SEADAS-8.1.0

SeaDAS Release: 8.0.0

    seadas-toolbox: 1.0.0		
    s3tbx:8.0.0		
    snap-desktop: SEADAS-8.0.0	
    snap-engine: SEADAS-8.0.0


