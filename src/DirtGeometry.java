import java.util.HashMap;

import javax.media.opengl.GL2;

public class DirtGeometry extends Geometry
{
	protected static HashMap<Integer, DirtGeometry> instances = new HashMap<Integer, DirtGeometry>();
	
	public static DirtGeometry getInstance(float size)
	{
		int key = floatToIntKey(size);
		DirtGeometry instance = instances.get(key);
		if(instance == null)
		{
			instance = new DirtGeometry(size);
			instances.put(key, instance);
		}
		
		return instance;
	}
	
	public DirtGeometry(float size)
	{
		width = size;
		height = size;
	}
	
	public void buildGeometry(float viewWidth, float viewHeight)
	{
		vertices = new float[4 * 3];
		vertices[0] = -width/2;
		vertices[1] = -height/2;
		vertices[2] = 0;
		vertices[3] = -width/2;
		vertices[4] = height/2;
		vertices[5] = 0;
		vertices[6] = width/2;
		vertices[7] = height/2;
		vertices[8] = 0;
		vertices[9] = width/2;
		vertices[10] = -height/2;
		vertices[11] = 0;
		
		drawMode = GL2.GL_POLYGON;
	}
	
	public static int floatToIntKey(float f)
	{
		return (int)(10000*f);
	}
}
