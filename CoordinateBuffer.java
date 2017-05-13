

/**
*   CoordinateBuffer class is for storing points that satisfies a certain criteria.
*   You can first store the points, and then ask for minimum, maximum and mean values
*   for the X and/or Y coordinates.
*/
public class CoordinateBuffer
{
    int _sumx, _sumy;
    int _count;

    int _minx, _miny, _maxx, _maxy;

    public CoordinateBuffer()
    {
        reset();
    }

    /**
    *   adds the point (x, y) to the buffer.
    */
    public void add(int x, int y)
    {
        _sumx += x;
        _sumy += y;
        _count++;

        if( x < _minx )         _minx = x;
        else if( x > _maxx )    _maxx = x;

        if( y < _miny )         _miny = y;
        else if( y > _maxy )    _maxy = y;
    }

    public double getMeanX()
    {
        return (_count>0)? ((double)_sumx)/_count : Double.NaN;
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

    /**
    *   reset what has been added to the buffer so far.
    */
    public void reset()
    {
        _sumx   = 0;
        _sumy   = 0;
        _count  = 0;

        _minx   = Integer.MAX_VALUE;
        _miny   = Integer.MAX_VALUE;
        _maxx   = -1;
        _maxy   = -1;
    }
}
