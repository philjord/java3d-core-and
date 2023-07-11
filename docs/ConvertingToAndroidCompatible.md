Converting to Android Compatible Code
===
Firstly, you need to convert your Java3D project to run with the jogl2es2 pipeline, see https://github.com/philjord/java3d-core/blob/master/docs/GL2ES2PipelineUsageGuide.md


##Concept
The Android runtime does not have the package java.awt at all. This means a huge number of basic classes like BufferedImage are no longer available for use in Java3D.
It also mean that the Canvas3D can no longer manage it's 3d window and treat it like a JPanel, so the users has to configure and move the 3d window themselves, which is trivial. Also the Canvas3D is not able to hand out GUI events so a new event listener must be registered on the 3d window (of type glWindow from Jogl). 

##How is this non AWT platform and conversion handled
blah balh little wee adpaters in a package javaawt.swing etc
The java3d-core-and project replaces a bunch of classes from java3d-core

###What changes to code are needed
Get your project sorted sorted like this:


Canvas3D init is like this, textures are like that, window event are like this, compressed texture are now ETC or ASTC

### thing that aren't supported any more





