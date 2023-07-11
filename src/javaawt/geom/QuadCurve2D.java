package javaawt.geom;

/**
 * The <code>QuadCurve2D</code> class defines a quadratic parametric curve
 * segment in {@code (x,y)} coordinate space.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D quadratic curve segment.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public  class QuadCurve2D {

 




    /**
     * Subdivides the quadratic curve specified by the coordinates
     * stored in the <code>src</code> array at indices
     * <code>srcoff</code> through <code>srcoff</code>&nbsp;+&nbsp;5
     * and stores the resulting two subdivided curves into the two
     * result arrays at the corresponding indices.
     * Either or both of the <code>left</code> and <code>right</code>
     * arrays can be <code>null</code> or a reference to the same array
     * and offset as the <code>src</code> array.
     * Note that the last point in the first subdivided curve is the
     * same as the first point in the second subdivided curve.  Thus,
     * it is possible to pass the same array for <code>left</code> and
     * <code>right</code> and to use offsets such that
     * <code>rightoff</code> equals <code>leftoff</code> + 4 in order
     * to avoid allocating extra storage for this common point.
     * @param src the array holding the coordinates for the source curve
     * @param srcoff the offset into the array of the beginning of the
     * the 6 source coordinates
     * @param left the array for storing the coordinates for the first
     * half of the subdivided curve
     * @param leftoff the offset into the array of the beginning of the
     * the 6 left coordinates
     * @param right the array for storing the coordinates for the second
     * half of the subdivided curve
     * @param rightoff the offset into the array of the beginning of the
     * the 6 right coordinates
     * @since 1.2
     */
    public static void subdivide(double src[], int srcoff,
                                 double left[], int leftoff,
                                 double right[], int rightoff) {
        double x1 = src[srcoff + 0];
        double y1 = src[srcoff + 1];
        double ctrlx = src[srcoff + 2];
        double ctrly = src[srcoff + 3];
        double x2 = src[srcoff + 4];
        double y2 = src[srcoff + 5];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
        x1 = (x1 + ctrlx) / 2.0;
        y1 = (y1 + ctrly) / 2.0;
        x2 = (x2 + ctrlx) / 2.0;
        y2 = (y2 + ctrly) / 2.0;
        ctrlx = (x1 + x2) / 2.0;
        ctrly = (y1 + y2) / 2.0;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx;
            left[leftoff + 5] = ctrly;
        }
        if (right != null) {
            right[rightoff + 0] = ctrlx;
            right[rightoff + 1] = ctrly;
            right[rightoff + 2] = x2;
            right[rightoff + 3] = y2;
        }
    }

    /**
     * Solves the quadratic whose coefficients are in the <code>eqn</code>
     * array and places the non-complex roots back into the same array,
     * returning the number of roots.  The quadratic solved is represented
     * by the equation:
     * <pre>
     *     eqn = {C, B, A};
     *     ax^2 + bx + c = 0
     * </pre>
     * A return value of <code>-1</code> is used to distinguish a constant
     * equation, which might be always 0 or never 0, from an equation that
     * has no zeroes.
     * @param eqn the array that contains the quadratic coefficients
     * @return the number of roots, or <code>-1</code> if the equation is
     *          a constant
     * @since 1.2
     */
    public static int solveQuadratic(double eqn[]) {
        return solveQuadratic(eqn, eqn);
    }

    /**
     * Solves the quadratic whose coefficients are in the <code>eqn</code>
     * array and places the non-complex roots into the <code>res</code>
     * array, returning the number of roots.
     * The quadratic solved is represented by the equation:
     * <pre>
     *     eqn = {C, B, A};
     *     ax^2 + bx + c = 0
     * </pre>
     * A return value of <code>-1</code> is used to distinguish a constant
     * equation, which might be always 0 or never 0, from an equation that
     * has no zeroes.
     * @param eqn the specified array of coefficients to use to solve
     *        the quadratic equation
     * @param res the array that contains the non-complex roots
     *        resulting from the solution of the quadratic equation
     * @return the number of roots, or <code>-1</code> if the equation is
     *  a constant.
     * @since 1.3
     */
    public static int solveQuadratic(double eqn[], double res[]) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
        int roots = 0;
        if (a == 0.0) {
            // The quadratic parabola has degenerated to a line.
            if (b == 0.0) {
                // The line has degenerated to a constant.
                return -1;
            }
            res[roots++] = -c / b;
        } else {
            // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
            double d = b * b - 4.0 * a * c;
            if (d < 0.0) {
                // If d < 0.0, then there are no roots
                return 0;
            }
            d = Math.sqrt(d);
            // For accuracy, calculate one root using:
            //     (-b +/- d) / 2a
            // and the other using:
            //     2c / (-b +/- d)
            // Choose the sign of the +/- so that b+d gets larger in magnitude
            if (b < 0.0) {
                d = -d;
            }
            double q = (b + d) / -2.0;
            // We already tested a for being 0 above
            res[roots++] = q / a;
            if (q != 0.0) {
                res[roots++] = c / q;
            }
        }
        return roots;
    }


    /**
     * Fill an array with the coefficients of the parametric equation
     * in t, ready for solving against val with solveQuadratic.
     * We currently have:
     *     val = Py(t) = C1*(1-t)^2 + 2*CP*t*(1-t) + C2*t^2
     *                 = C1 - 2*C1*t + C1*t^2 + 2*CP*t - 2*CP*t^2 + C2*t^2
     *                 = C1 + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
     *               0 = (C1 - val) + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
     *               0 = C + Bt + At^2
     *     C = C1 - val
     *     B = 2*CP - 2*C1
     *     A = C1 - 2*CP + C2
     */
    private static void fillEqn(double eqn[], double val,
                                double c1, double cp, double c2) {
        eqn[0] = c1 - val;
        eqn[1] = cp + cp - c1 - c1;
        eqn[2] = c1 - cp - cp + c2;
        return;
    }

    /**
     * Evaluate the t values in the first num slots of the vals[] array
     * and place the evaluated values back into the same array.  Only
     * evaluate t values that are within the range <0, 1>, including
     * the 0 and 1 ends of the range iff the include0 or include1
     * booleans are true.  If an "inflection" equation is handed in,
     * then any points which represent a point of inflection for that
     * quadratic equation are also ignored.
     */
    private static int evalQuadratic(double vals[], int num,
                                     boolean include0,
                                     boolean include1,
                                     double inflect[],
                                     double c1, double ctrl, double c2) {
        int j = 0;
        for (int i = 0; i < num; i++) {
            double t = vals[i];
            if ((include0 ? t >= 0 : t > 0) &&
                (include1 ? t <= 1 : t < 1) &&
                (inflect == null ||
                 inflect[1] + 2*inflect[2]*t != 0))
            {
                double u = 1 - t;
                vals[j++] = c1*u*u + 2*ctrl*t*u + c2*t*t;
            }
        }
        return j;
    }

    private static final int BELOW = -2;
    private static final int LOWEDGE = -1;
    private static final int INSIDE = 0;
    private static final int HIGHEDGE = 1;
    private static final int ABOVE = 2;

    /**
     * Determine where coord lies with respect to the range from
     * low to high.  It is assumed that low <= high.  The return
     * value is one of the 5 values BELOW, LOWEDGE, INSIDE, HIGHEDGE,
     * or ABOVE.
     */
    private static int getTag(double coord, double low, double high) {
        if (coord <= low) {
            return (coord < low ? BELOW : LOWEDGE);
        }
        if (coord >= high) {
            return (coord > high ? ABOVE : HIGHEDGE);
        }
        return INSIDE;
    }

    /**
     * Determine if the pttag represents a coordinate that is already
     * in its test range, or is on the border with either of the two
     * opttags representing another coordinate that is "towards the
     * inside" of that test range.  In other words, are either of the
     * two "opt" points "drawing the pt inward"?
     */
    private static boolean inwards(int pttag, int opt1tag, int opt2tag) {
        switch (pttag) {
        case BELOW:
        case ABOVE:
        default:
            return false;
        case LOWEDGE:
            return (opt1tag >= INSIDE || opt2tag >= INSIDE);
        case INSIDE:
            return true;
        case HIGHEDGE:
            return (opt1tag <= INSIDE || opt2tag <= INSIDE);
        }
    }
    /**
     * Returns the square of the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the indicated control points.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     * @return the square of the flatness of the quadratic curve
     *          defined by the specified coordinates.
     * @since 1.2
     */
    public static double getFlatnessSq(double x1, double y1,
                                       double ctrlx, double ctrly,
                                       double x2, double y2) {
        return Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly);
    }

    

    /**
     * Returns the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the indicated control points.
     *
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param ctrlx the X coordinate of the control point
     * @param ctrly the Y coordinate of the control point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     * @return the flatness of the quadratic curve defined by the
     *          specified coordinates.
     * @since 1.2
     */
    public static double getFlatness(double x1, double y1,
                                     double ctrlx, double ctrly,
                                     double x2, double y2) {
        return Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly);
    }

    /**
     * Returns the square of the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the control points stored in the
     * indicated array at the indicated index.
     * @param coords an array containing coordinate values
     * @param offset the index into <code>coords</code> from which to
     *          to start getting the values from the array
     * @return the flatness of the quadratic curve that is defined by the
     *          values in the specified array at the specified index.
     * @since 1.2
     */
    public static double getFlatnessSq(double coords[], int offset) {
        return Line2D.ptSegDistSq(coords[offset + 0], coords[offset + 1],
                                  coords[offset + 4], coords[offset + 5],
                                  coords[offset + 2], coords[offset + 3]);
    }

    /**
     * Returns the flatness, or maximum distance of a
     * control point from the line connecting the end points, of the
     * quadratic curve specified by the control points stored in the
     * indicated array at the indicated index.
     * @param coords an array containing coordinate values
     * @param offset the index into <code>coords</code> from which to
     *          start getting the coordinate values
     * @return the flatness of a quadratic curve defined by the
     *          specified array at the specified offset.
     * @since 1.2
     */
    public static double getFlatness(double coords[], int offset) {
        return Line2D.ptSegDist(coords[offset + 0], coords[offset + 1],
                                coords[offset + 4], coords[offset + 5],
                                coords[offset + 2], coords[offset + 3]);
    }



}

