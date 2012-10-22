/**
 * Don't use directly. Subclasses should always be singleton.
 */
public class Geometry
{
	public float[] vertices = null;
	
	public int vertexBufferID = 0;
	public int positionBufferID = 0;
	
	public int drawMode;

	protected float width = 0;
	protected float height = 0;
	
	protected static Geometry instance = null;
	
	public static Geometry getInstance()
	{
		if(instance == null)
		{
			instance = new Geometry();
		}
		
		return instance;
	}
	
	// subclasses are expected to override this method and put something in vertices 
	public void buildGeometry(float viewWidth, float viewHeight) {}

	public int getNumPoints() 
	{
		return vertices.length/2;
	}
}
