package javaawt;


import javaawt.geom.AffineTransform;
import javaawt.geom.PathIterator;
import javaawt.geom.Point2D;
import javaawt.geom.Rectangle2D;
 


public interface Shape {
    
    public Rectangle getBounds();

    
    public Rectangle2D getBounds2D();

    
    public boolean contains(double x, double y);

    
    public boolean contains(Point2D p);

    
    public boolean intersects(double x, double y, double w, double h);

    
    public boolean intersects(Rectangle2D r);

   
    public boolean contains(double x, double y, double w, double h);

  
    public boolean contains(Rectangle2D r);


    public PathIterator getPathIterator(AffineTransform at);

   
    public PathIterator getPathIterator(AffineTransform at, double flatness);
}

