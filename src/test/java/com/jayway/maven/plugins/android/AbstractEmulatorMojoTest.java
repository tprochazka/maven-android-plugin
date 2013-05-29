package com.jayway.maven.plugins.android;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ AndroidDebugBridge.class, CommandExecutor.Factory.class } )
public class AbstractEmulatorMojoTest
{

    private static final String AVD_NAME = "emulator";
    private static final int WAIT = 500;
    private AbstractEmulatorMojo abstractEmulatorMojo;
    private CommandExecutor mockExecutor;
    private AndroidDebugBridge mockAndroidDebugBridge;

    @Before
    public void setUp() throws Exception
    {
        mockExecutor = PowerMock.createNiceMock( CommandExecutor.class );
        mockExecutor.executeCommand( anyObject( String.class ), isNull( List.class ) );
        PowerMock.replay( mockExecutor );

        mockStatic( CommandExecutor.Factory.class );
        expect( CommandExecutor.Factory.createDefaultCommmandExecutor() ).andReturn( mockExecutor );
        PowerMock.replay( CommandExecutor.Factory.class );

        mockAndroidDebugBridge = createMock( AndroidDebugBridge.class );

        abstractEmulatorMojo = new AbstractEmulatorMojoToTest();
    }

    @Test
    public void testStartAndroidEmulatorSuccessfully() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = true;
        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry );
        withConnectedDebugBridge( emulatorDevice );

        abstractEmulatorMojo.startAndroidEmulator();

        verify( mockExecutor );
    }

    @Test
    public void testStartAndroidEmulatorWithTimeoutToConnect() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = false;
        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry );
        withConnectedDebugBridge( emulatorDevice );

        try
        {
            abstractEmulatorMojo.startAndroidEmulator();
            fail();
        }
        catch ( MojoExecutionException e )
        {
            verify( mockExecutor );
        }

    }

    private IDevice withEmulatorDevice( boolean onlineAtSecondTry )
    {
        IDevice emulatorDevice = createMock( IDevice.class );
        expect( emulatorDevice.getAvdName() ).andReturn( AVD_NAME ).atLeastOnce();
        expect( emulatorDevice.isEmulator() ).andReturn( true ).atLeastOnce();
        if ( onlineAtSecondTry )
        {
            expect( emulatorDevice.isOnline() ).andReturn( false ).andReturn( true );
        }
        else
        {
            expect( emulatorDevice.isOnline() ).andReturn( false ).atLeastOnce();
        }
        replay( emulatorDevice );
        return emulatorDevice;
    }

    private void withConnectedDebugBridge( IDevice emulatorDevice )
    {
        expect( mockAndroidDebugBridge.isConnected() ).andReturn( true );
        expect( mockAndroidDebugBridge.hasInitialDeviceList() ).andReturn( true );
        expect( mockAndroidDebugBridge.getDevices() ).andReturn( new IDevice[ 0 ] ).andReturn( new IDevice[]
        { emulatorDevice } ).atLeastOnce();
        replay( mockAndroidDebugBridge );
    }

    private class AbstractEmulatorMojoToTest extends AbstractEmulatorMojo
    {

        @Override
        protected AndroidSdk getAndroidSdk() {
            return new SdkTestSupport().getSdk_with_platform_default();
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException
        {
        }

        @Override
        protected AndroidDebugBridge initAndroidDebugBridge() throws MojoExecutionException
        {
            return mockAndroidDebugBridge;
        }

        @Override
        String determineAvd()
        {
            return AVD_NAME;
        }

        @Override
        String determineWait()
        {
            return String.valueOf( WAIT );
        }
    }

}
