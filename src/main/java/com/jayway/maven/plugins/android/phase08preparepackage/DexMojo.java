/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.phase08preparepackage;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.Const;
import com.jayway.maven.plugins.android.common.ZipExtractor;
import com.jayway.maven.plugins.android.configuration.Dex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

/**
 * Converts compiled Java classes to the Android dex format.
 * 
 * @author hugo.josefson@jayway.com
 * @goal dex
 * @phase prepare-package
 * @requiresDependencyResolution compile
 */
public class DexMojo extends AbstractAndroidMojo
{

    /**
     * Configuration for the dex command execution. It can be configured in the plugin configuration like so
     * 
     * <pre>
     * &lt;dex&gt;
     *   &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     *   &lt;coreLibrary&gt;true|false&lt;/coreLibrary&gt;
     *   &lt;noLocals&gt;true|false&lt;/noLocals&gt;
     *   &lt;forceJumbo&gt;true|false&lt;/forceJumbo&gt;
     *   &lt;optimize&gt;true|false&lt;/optimize&gt;
     *   &lt;preDex&gt;true|false&lt;/preDex&gt;
     *   &lt;preDexLibLocation&gt;path to predexed libraries, defaults to target/dexedLibs&lt;/preDexLibLocation&gt;
     *   &lt;incremental&gt;true|false&lt;/incremental&gt;
     * &lt;/dex&gt;
     * </pre>
     * <p/>
     * or via properties dex.* or command line parameters android.dex.*
     * 
     * @parameter
     */
    private Dex dex;
    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     * 
     * @parameter property="android.dex.jvmArguments" default-value="-Xmx1024M"
     * @optional
     */
    private String[] dexJvmArguments;

    /**
     * Decides whether to pass the --core-library flag to dx.
     * 
     * @parameter property="android.dex.coreLibrary" default-value="false"
     */
    private boolean dexCoreLibrary;

    /**
     * Decides whether to pass the --no-locals flag to dx.
     * 
     * @parameter property="android.dex.noLocals" default-value="false"
     */
    private boolean dexNoLocals;

    /**
     * Decides whether to pass the --no-optimize flag to dx.
     * 
     * @parameter property="android.dex.optimize" default-value="true"
     */
    private boolean dexOptimize;

    /**
     * Decides whether to predex the jars.
     * 
     * @parameter property="android.dex.predex" default-value="false"
     */
    private boolean dexPreDex;
    
    /**
     * Decides whether to use force jumbo mode.
     * 
     * @parameter property="android.dex.forcejumbo" default-value="false"
     */
    private boolean dexForceJumbo;

    /**
     * Path to predexed libraries.
     * 
     * @parameter property="android.dex.dexPreDexLibLocation" default-value=
     *            "${project.build.directory}${file.separator}dexedLibs"
     */
    private String dexPreDexLibLocation;

    /**
     * Decides whether to pass the --incremental flag to dx.
     *
     * @parameter property="android.dex.incremental" default-value="false"
     */
    private boolean dexIncremental;

    /**
     * The name of the obfuscated JAR
     * @parameter property="android.proguard.obfuscatedJar"
     */
    private File obfuscatedJar;

    private String[] parsedJvmArguments;
    private boolean parsedCoreLibrary;
    private boolean parsedNoLocals;
    private boolean parsedOptimize;
    private boolean parsedPreDex;
    private boolean parsedForceJumbo;
    private String parsedPreDexLibLocation;
    private boolean parsedIncremental;

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        File outputFile = new File( project.getBuild().getDirectory() + File.separator + "classes.dex" );

        parseConfiguration();

        if ( generateApk )
        {
            runDex( executor, outputFile );
        }

        if ( attachJar )
        {
            File jarFile = new File( project.getBuild().getDirectory() + File.separator
                    + project.getBuild().getFinalName() + ".jar" );
            projectHelper.attachArtifact( project, "jar", project.getArtifact().getClassifier(), jarFile );
        }

        if ( attachSources )
        {
            // Also attach an .apksources, containing sources from this project.
            final File apksources = createApkSourcesFile();
            projectHelper.attachArtifact( project, "apksources", apksources );
        }
    }

    /**
     * Gets the input files for dex. This is a combination of directories and jar files.
     * 
     * @return
     */
    private Set< File > getDexInputFiles() throws MojoExecutionException
    {
        Set< File > inputs = new HashSet< File >();

        if ( obfuscatedJar != null && obfuscatedJar.exists() )
        {
            // proguard has been run, use this jar
            getLog().debug( "Adding dex input (obfuscatedJar) : " + obfuscatedJar );
            inputs.add( obfuscatedJar );
        }
        else
        {
            getLog().debug( "Using non-obfuscated input" );
            // no proguard, use original config
            inputs.add( new File( project.getBuild().getOutputDirectory() ) );
            getLog().debug( "Adding dex input : " + project.getBuild().getOutputDirectory() );
            for ( Artifact artifact : getTransitiveDependencyArtifacts() )
            {
                if ( artifact.getType().equals( Const.ArtifactType.NATIVE_SYMBOL_OBJECT )
                        || artifact.getType().equals( Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE ) )
                {
                    // Ignore native dependencies - no need for dexer to see those
                    continue;
                }
                else if ( artifact.getType().equals( APKLIB ) )
                {
                    // No classes in an APKLIB to dex.
                    // Any potential classes will have already bee compiled to target/classes
                    continue;
                }
                else if ( artifact.getType().equals( AAR ) )
                {
                    // We need to get the aar classes, not the aar itself.
                    final File jar = getUnpackedAarClassesJar( artifact );
                    getLog().debug( "Adding dex input : " + jar );
                    inputs.add( jar.getAbsoluteFile() );
                }
                else if ( artifact.getType().equals( APK ) )
                {
                    // We need to dex the APK classes including the APK R.
                    // But we don't want to add a second instance of the embedded Rs for any of the APK's dependencies
                    // as they will already have been generated to target/classes. The R values from the APK will be
                    // the correct ones, so best solution is to extract the APK classes (including all Rs) to
                    // target/classes overwriting any generated Rs and let dex pick up the values from there.
                    getLog().debug( "Extracting APK classes to target/classes : " + artifact.getArtifactId() );
                    final File apkClassesJar = getUnpackedLibHelper().getJarFileForApk( artifact );
                    getLog().debug( "Extracting APK : " + apkClassesJar + " to " + targetDirectory );
                    final ZipExtractor extractor = new ZipExtractor( getLog() );
                    extractor.extract( apkClassesJar, targetDirectory, ".class" );
                }
                else
                {
                    getLog().debug( "Adding dex input : " + artifact.getFile() );
                    inputs.add( artifact.getFile().getAbsoluteFile() );
                }
            }
        }

        return inputs;
    }

    private void parseConfiguration()
    {
        // config in pom found
        if ( dex != null )
        {
            // the if statements make sure that properties/command line
            // parameter overrides configuration
            // and that the dafaults apply in all cases;
            if ( dex.getJvmArguments() == null )
            {
                parsedJvmArguments = dexJvmArguments;
            }
            else
            {
                parsedJvmArguments = dex.getJvmArguments();
            }
            if ( dex.isCoreLibrary() == null )
            {
                parsedCoreLibrary = dexCoreLibrary;
            }
            else
            {
                parsedCoreLibrary = dex.isCoreLibrary();
            }
            if ( dex.isNoLocals() == null )
            {
                parsedNoLocals = dexNoLocals;
            }
            else
            {
                parsedNoLocals = dex.isNoLocals();
            }
            if ( dex.isOptimize() == null )
            {
                parsedOptimize = dexOptimize;
            }
            else
            {
                parsedOptimize = dex.isOptimize();
            }
            if ( dex.isPreDex() == null )
            {
                parsedPreDex = dexPreDex;
            }
            else
            {
                parsedPreDex = dex.isPreDex();
            }
            if ( dex.getPreDexLibLocation() == null )
            {
                parsedPreDexLibLocation = dexPreDexLibLocation;
            }
            else
            {
                parsedPreDexLibLocation = dex.getPreDexLibLocation();
            }
            if ( dex.isIncremental() == null )
            {
                parsedIncremental = dexIncremental;
            }
            else
            {
                parsedIncremental = dex.isIncremental();
            }
            if ( dex.isForceJumbo() == null )
            {
                parsedForceJumbo = dexForceJumbo;
            }
            else
            {
                parsedForceJumbo = dex.isForceJumbo();
            }
        }
        else
        {
            parsedJvmArguments = dexJvmArguments;
            parsedCoreLibrary = dexCoreLibrary;
            parsedNoLocals = dexNoLocals;
            parsedOptimize = dexOptimize;
            parsedPreDex = dexPreDex;
            parsedPreDexLibLocation = dexPreDexLibLocation;
            parsedIncremental = dexIncremental;
            parsedForceJumbo = dexForceJumbo;
        }
    }

    private Set< File > preDex( CommandExecutor executor, Set< File > inputFiles ) throws MojoExecutionException
    {
        Set< File > filtered = new HashSet< File >();
        getLog().info( "Pre dex-ing libraries for faster dex-ing of the final application." );

        for ( File inputFile : inputFiles )
        {
            if ( inputFile.getName().matches( ".*\\.jar$" ) )
            {
                List< String > commands = dexDefaultCommands();

                File predexJar = predexJarPath( inputFile );
                commands.add( "--output=" + predexJar.getAbsolutePath() );
                commands.add( inputFile.getAbsolutePath() );
                filtered.add( predexJar );

                if ( !predexJar.isFile() || predexJar.lastModified() < inputFile.lastModified() )
                {
                    getLog().info( "Pre-dex ing jar: " + inputFile.getAbsolutePath() );

                    final String javaExecutable = getJavaExecutable().getAbsolutePath();
                    getLog().debug( javaExecutable + " " + commands.toString() );
                    try
                    {
                        executor.setCaptureStdOut( true );
                        executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
                    }
                    catch ( ExecutionException e )
                    {
                        throw new MojoExecutionException( "", e );
                    }
                }

            }
            else
            {
                filtered.add( inputFile );
            }
        }

        return filtered;
    }

    private File predexJarPath( File inputFile )
    {
        final File predexLibsDirectory = new File( parsedPreDexLibLocation.trim() );
        predexLibsDirectory.mkdirs();
        return new File( predexLibsDirectory, inputFile.getName() );
    }

    private List< String > dexDefaultCommands() throws MojoExecutionException
    {

        List< String > commands = new ArrayList< String >();
        if ( parsedJvmArguments != null )
        {
            for ( String jvmArgument : parsedJvmArguments )
            {
                // preserve backward compatibility allowing argument with or
                // without dash (e.g. Xmx512m as well as
                // -Xmx512m should work) (see
                // http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                if ( !jvmArgument.startsWith( "-" ) )
                {
                    jvmArgument = "-" + jvmArgument;
                }
                getLog().debug( "Adding jvm argument " + jvmArgument );
                commands.add( jvmArgument );
            }
        }
        commands.add( "-jar" );
        commands.add( getAndroidSdk().getDxJarPath() );
        commands.add( "--dex" );

        return commands;

    }

    private void runDex( CommandExecutor executor, File outputFile )
            throws MojoExecutionException
    {
        final List< String > commands = dexDefaultCommands();
        final Set< File > inputFiles = getDexInputFiles();
        Set< File > filteredFiles = inputFiles;

        if ( parsedPreDex )
        {
            filteredFiles = preDex( executor, inputFiles );
        }
        if ( !parsedOptimize )
        {
            commands.add( "--no-optimize" );
        }
        if ( parsedCoreLibrary )
        {
            commands.add( "--core-library" );
        }
        if ( parsedIncremental )
        {
            commands.add( "--incremental" );
        }
        commands.add( "--output=" + outputFile.getAbsolutePath() );
        if ( parsedNoLocals )
        {
            commands.add( "--no-locals" );
        }
        if ( parsedForceJumbo )
        {
            commands.add( "--force-jumbo" );
        }

        for ( File inputFile : filteredFiles )
        {
            commands.add( inputFile.getAbsolutePath() );
        }

        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().debug( javaExecutable + " " + commands.toString() );
        getLog().info( "Convert classes to Dex : " + outputFile );
        try
        {
            executor.setCaptureStdOut( true );
            executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }

    /**
     * Figure out the full path to the current java executable.
     * 
     * @return the full path to the current java executable.
     */
    private static File getJavaExecutable()
    {
        final String javaHome = System.getProperty( "java.home" );
        final String slash = File.separator;
        return new File( javaHome + slash + "bin" + slash + "java" );
    }

    /**
     * @return
     * @throws MojoExecutionException
     */
    protected File createApkSourcesFile() throws MojoExecutionException
    {
        final File apksources = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName()
                + ".apksources" );
        FileUtils.deleteQuietly( apksources );

        try
        {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile( apksources );

            addDirectory( jarArchiver, assetsDirectory, "assets" );
            addDirectory( jarArchiver, resourceDirectory, "res" );
            addDirectory( jarArchiver, sourceDirectory, "src/main/java" );
            addJavaResources( jarArchiver, project.getBuild().getResources() );

            jarArchiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while creating .apksource file.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while creating .apksource file.", e );
        }

        return apksources;
    }

    /**
     * Makes sure the string ends with "/"
     * 
     * @param prefix
     *            any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash( String prefix )
    {
        prefix = StringUtils.defaultIfEmpty( prefix, "/" );
        if ( !prefix.endsWith( "/" ) )
        {
            prefix = prefix + "/";
        }
        return prefix;
    }

    /**
     * Adds a directory to a {@link JarArchiver} with a directory prefix.
     * 
     * @param jarArchiver
     * @param directory
     *            The directory to add.
     * @param prefix
     *            An optional prefix for where in the Jar file the directory's contents should go.
     */
    protected void addDirectory( JarArchiver jarArchiver, File directory, String prefix )
    {
        if ( directory != null && directory.exists() )
        {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix( endWithSlash( prefix ) );
            fileSet.setDirectory( directory );
            jarArchiver.addFileSet( fileSet );
        }
    }

    /**
     * @param jarArchiver
     * @param javaResources
     */
    protected void addJavaResources( JarArchiver jarArchiver, List< Resource > javaResources )
    {
        for ( Resource javaResource : javaResources )
        {
            addJavaResource( jarArchiver, javaResource );
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link JarArchiver}.
     * 
     * @param jarArchiver
     * @param javaResource
     *            The Java resource to add.
     */
    protected void addJavaResource( JarArchiver jarArchiver, Resource javaResource )
    {
        if ( javaResource != null )
        {
            final File javaResourceDirectory = new File( javaResource.getDirectory() );
            if ( javaResourceDirectory.exists() )
            {
                final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                javaResourceFileSet.setDirectory( javaResourceDirectory );
                javaResourceFileSet.setPrefix( endWithSlash( "src/main/resources" ) );
                jarArchiver.addFileSet( javaResourceFileSet );
            }
        }
    }
}
