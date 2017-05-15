
/**
*   CoordinateBuffer class is for storing points that satisfies a certain criteria.
*   You can first store the points, and then ask for minimum, maximum and mean values
*   for the X and/or Y coordinates.
*/
public class CoordinateBuffer
{
    static final int ORDER = 2;

    int[] _sumx  = new int[ORDER];
    int   _sumy  = 0;
    int   _sumxy = 0;
    int _count;

    double _a, _b, _theta, _deg;

    int _minx, _miny, _maxx, _maxy;
    boolean _calculated;

    public CoordinateBuffer()
    {
        reset();
    }

    /**
    *   adds the point (x, y) to the buffer.
    */
    public void add(int x, int y)
    {
        _sumx[0] += x;
        _sumx[1] += x*x;
        _sumy    += y;
        _sumxy   += x*y;
        _count++;

        if( x < _minx )         _minx = x;
        else if( x > _maxx )    _maxx = x;

        if( y < _miny )         _miny = y;
        else if( y > _maxy )    _maxy = y;

        if( _calculated )
            _calculated = false;
    }

    public double getMeanX()
    {
        return (_count>0)? ((double)_sumx[0])/_count : Double.NaN;
    }

    public double getMeanY()
    {
        return (_count>0)? ((double)_sumy)/_count : Double.NaN;
    }

    public int getMinX()
    {
        return (_count>0)? _minx : -1;
    }

    public int getMinY()
    {
        return (_count>0)? _miny : -1;
    }

    public int getMaxX()
    {
        return (_count>0)? _maxx : -1;
    }

    public int getMaxY()
    {
        return (_count>0)? _maxy : -1;
    }

    public double getA()
    {
        if( !_calculated )
            calculate();
        return _a;
    }

    public double getB()
    {
        if( !_calculated )
            calculate();
        return _b;
    }

    public double getRadian()
    {
        if( !_calculated )
            calculate();
        return _theta;
    }

    public double getDegrees()
    {        
        if( !_calculated )
            calculate();
        return _deg;
    }

    private void calculate(){
        double den    = (_count*(long)(_sumx[1]) - ((long)(_sumx[0]))*(_sumx[0]))*1.0;
        double anom   = (_count*(long)(_sumxy) - ((long)(_sumx[0]))*_sumy)*1.0;
        double bnom   = (((long)(_sumx[1]))*_sumy - ((long)(_sumx[0]))*_sumxy)*1.0;
        _a      = anom/den;
        _b      = bnom/den;
        _theta  = Math.atan2(anom, den);
        _deg    = Math.toDegrees(_theta);

        _calculated = true;
    }

    /**
    *   reset what has been added to the buffer so far.
    */
    public void reset()
    {
        for(int i=0; i<ORDER; i++){
            _sumx[i]   = 0;
        }
        _sumy   = 0;
        _sumxy  = 0;
        _count  = 0;

        _minx   = Integer.MAX_VALUE;
        _miny   = Integer.MAX_VALUE;
        _maxx   = -1;
        _maxy   = -1;
        _a      = Double.NaN;
        _b      = Double.NaN;
        _theta  = Double.NaN;
        _deg    = Double.NaN;
        _calculated = false;
    }
}
