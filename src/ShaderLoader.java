import java.io.FileInputStream;
import java.io.IOException;
import javax.media.opengl.GL2;

/**
 * Reads, compiles, and loads shaders from files.
 * 
 * @author Zeb Long
 */
public class ShaderLoader
{
	private static final String PATH = "shaders/";

	/**
	 * Reads a file and returns the contents as a String
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static String readShaderFromFile(String filename) throws IOException
	{
		FileInputStream f = new FileInputStream(PATH+filename);
		
		int size = (int)(f.getChannel().size());
		byte[] source = new byte[size];
		
		f.read(source,0,size);
		
		f.close();
		
		return new String(source,0,size);
	}
	
	/**
	 * Compiles some shader code and binds it to the provided shaderID
	 * 
	 * @param gl
	 * @param shaderCode
	 * @param shaderID
	 */
	public static void compileShader(GL2 gl, String shaderCode, int shaderID)
	{
		String[] lines = new String[] { shaderCode };
        int[] vlengths = new int[] { lines[0].length() };
        gl.glShaderSource(shaderID, lines.length, lines, vlengths, 0);
        gl.glCompileShader(shaderID);
        
        //Check compile status.
        int[] compiled = new int[1];
        gl.glGetShaderiv(shaderID, GL2.GL_COMPILE_STATUS, compiled, 0);
        if(compiled[0] != 0)
        {
        	System.out.println("Horray! shader compiled");
        }
        else 
        {
            int[] logLength = new int[1];
            gl.glGetShaderiv(shaderID, GL2.GL_INFO_LOG_LENGTH, logLength, 0);

            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(shaderID, logLength[0], (int[])null, 0, log, 0);

            System.err.println("Error compiling the vertex shader: " + new String(log));
            System.exit(1);
        }
	}
	
	/**
	 * Compiles the shaders into a program and returns the program's id
	 * The shader code comes from two files:
	 * 	[PATH]/[shaderName].vertex 
	 * and 
	 *  [PATH]/[shaderName].fragment
	 * 
	 * @param gl
	 * @param shaderName
	 * 
	 * @return the program id for use with glUseProgram
	 */
	public static int compileProgram(GL2 gl, String shaderName)
	{
		// generate shader ids
        int vertexShader   = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        
        String vertexShaderCode   = "";
        String fragmentShaderCode = "";
        
		try
		{
			vertexShaderCode   = readShaderFromFile(shaderName+".vertex");
			fragmentShaderCode = readShaderFromFile(shaderName+".fragment");
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
        
		// bind the compiled code to the generated ids
        compileShader(gl, vertexShaderCode, vertexShader);
        compileShader(gl, fragmentShaderCode, fragmentShader);

        // Attach the shaders to a program
        int shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        
        return shaderProgram;
	}
}
