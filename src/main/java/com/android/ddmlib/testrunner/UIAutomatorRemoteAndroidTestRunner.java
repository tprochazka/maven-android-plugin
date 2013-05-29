package com.android.ddmlib.testrunner;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

/**
 * Runs a UI Automator test command remotely and reports results.
 */
public class UIAutomatorRemoteAndroidTestRunner
{

    private IDevice mRemoteDevice;
    // default to no timeout
    private int mMaxTimeToOutputResponse = 0;
    private String mRunName = null;

    /** map of name-value instrumentation argument pairs */
    private List< Entry< String, String >> mArgList;
    private InstrumentationResultParser mParser;
    private final String jarFile;
    private boolean noHup;
    private Object dumpFilePath;

    private static final String LOG_TAG = "RemoteAndroidTest";

    // defined instrumentation argument names
    private static final String CLASS_ARG_NAME = "class";
    private static final String DEBUG_ARG_NAME = "debug";

    public UIAutomatorRemoteAndroidTestRunner( String jarFile, IDevice remoteDevice )
    {
        this.jarFile = jarFile;
        mRemoteDevice = remoteDevice;
        mArgList = new ArrayList< Entry< String, String >>();
    }

    /**
     * {@inheritDoc}
     */

    public void setTestClassOrMethods( String[] testClassOrMethods )
    {
        for ( String testClassOrMethod : testClassOrMethods )
        {
            addInstrumentationArg( CLASS_ARG_NAME, testClassOrMethod );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addInstrumentationArg( String name, String value )
    {
        if ( name == null || value == null )
        {
            throw new IllegalArgumentException( "name or value arguments cannot be null" );
        }
        mArgList.add( new AbstractMap.SimpleImmutableEntry< String, String >( name, value ) );
    }

    /**
     * {@inheritDoc}
     */

    public void addBooleanArg( String name, boolean value )
    {
        addInstrumentationArg( name, Boolean.toString( value ) );
    }

    /**
     * {@inheritDoc}
     */

    /**
     * {@inheritDoc}
     */

    public void setDebug( boolean debug )
    {
        addBooleanArg( DEBUG_ARG_NAME, debug );
    }

    public void setNoHup( boolean noHup )
    {
        this.noHup = noHup;
    }

    public void setDumpFilePath( String dumpFilePath )
    {
        this.dumpFilePath = dumpFilePath;
    }

    /**
     * {@inheritDoc}
     */

    public void setMaxtimeToOutputResponse( int maxTimeToOutputResponse )
    {
        mMaxTimeToOutputResponse = maxTimeToOutputResponse;
    }

    /**
     * {@inheritDoc}
     */

    public void setRunName( String runName )
    {
        mRunName = runName;
    }

    /**
     * {@inheritDoc}
     */

    public void run( ITestRunListener... listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        run( Arrays.asList( listeners ) );
    }

    /**
     * {@inheritDoc}
     */
    public void run( Collection< ITestRunListener > listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        final String runCaseCommandStr = String.format( "uiautomator runtest %1$s %2$s", jarFile, buildArgsCommand() );
        Log.i( LOG_TAG, String.format( "Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber() ) );
        mParser = new InstrumentationResultParser( mRunName, listeners );

        try
        {
            mRemoteDevice.executeShellCommand( runCaseCommandStr, mParser, mMaxTimeToOutputResponse );
        }
        catch ( IOException e )
        {
            Log.w( LOG_TAG, String.format( "IOException %1$s when running tests %2$s on %3$s", e.toString(), jarFile,
                    mRemoteDevice.getSerialNumber() ) );
            // rely on parser to communicate results to listeners
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( ShellCommandUnresponsiveException e )
        {
            Log.w( LOG_TAG,
                    String.format( "ShellCommandUnresponsiveException %1$s when running tests %2$s on %3$s",
                            e.toString(), jarFile, mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( String.format( "Failed to receive adb shell test output within %1$d ms. "
                    + "Test may have timed out, or adb connection to device became unresponsive",
                    mMaxTimeToOutputResponse ) );
            throw e;
        }
        catch ( TimeoutException e )
        {
            Log.w( LOG_TAG,
                    String.format( "TimeoutException when running tests %1$s on %2$s", jarFile,
                            mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( AdbCommandRejectedException e )
        {
            Log.w( LOG_TAG, String.format( "AdbCommandRejectedException %1$s when running tests %2$s on %3$s",
                    e.toString(), jarFile, mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel()
    {
        if ( mParser != null )
        {
            mParser.cancel();
        }
    }

    /**
     * Returns the full instrumentation command line syntax for the provided instrumentation arguments. Returns an empty
     * string if no arguments were specified.
     */
    private String buildArgsCommand()
    {
        StringBuilder commandBuilder = new StringBuilder();
        for ( Entry< String, String > argPair : mArgList )
        {
            final String argCmd = String.format( " -e %1$s %2$s", argPair.getKey(), argPair.getValue() );
            commandBuilder.append( argCmd );
        }

        if ( noHup )
        {
            commandBuilder.append( " --nohup" );
        }

        if ( dumpFilePath != null )
        {
            commandBuilder.append( " dump " + dumpFilePath );
        }
        return commandBuilder.toString();
    }
    
    /**
     * Adds instrumentation arguments from userProperties from keys with the propertiesKeyPrefix prefix.
     * 
     * @param userProperties
     * @param propertiesKeyPrefix
     */
    public void setUserProperties( Properties userProperties, String propertiesKeyPrefix )
    {
        if ( userProperties == null )
        {
            throw new IllegalArgumentException( "userProperties  cannot be null" );
        }
        
        if ( StringUtils.isBlank( propertiesKeyPrefix ) )
        {
            //propertiesPrefix is blank, ignore all properties
            return;
        }
        
        for ( Entry< Object, Object > property : userProperties.entrySet() )
        {
            String name = (String) property.getKey();
            
            //Check if the key starts with the parameterPrefix
            if ( StringUtils.startsWith( name, propertiesKeyPrefix ) )
            {
                String value = (String) property.getValue();
                
                //Remove the prefix
                name = StringUtils.substring( name,
                        StringUtils.length( propertiesKeyPrefix ),
                        StringUtils.length( name ) );
                
                // Verify so the key isn't blank after substring
                if ( StringUtils.isNotBlank( name ) )
                {
                    //Now its safe to add the parameter
                    addInstrumentationArg( name, value );
                }
            }
        }
    }
}
