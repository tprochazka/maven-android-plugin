package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Lint;

/**
 * LintMojo can run the lint command against the project. Implements parsing parameters from pom or command line
 * arguments and sets useful defaults as well. Warning, if you use android.lint.enableClasspath and/or
 * android.lint.enableLibraries the behavior of this goal will vary depending on the phase where this goal is executed.
 * See android.lint.classpath/lintClassPath and android.lint.libraries/lintLibraries for more details.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal lint
 * @requiresProject false
 */
@SuppressWarnings( "unused" )
public class LintMojo extends AbstractAndroidMojo
{

    /**
     * The configuration for the lint goal. As soon as a lint goal is invoked the command will be executed unless the
     * skip parameter is set. A minimal configuration that will run lint and produce a XML report in
     * ${project.build.directory}/lint/lint-results.xml is
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;skip&gt;false&lt;/skip&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * Full configuration can use these parameters.
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;failOnError&gt;true|false&lt;/failOnError&gt;
     *     &lt;skip&gt;true|false&lt;/skip&gt;
     *     &lt;ignoreWarnings&gt;true|false&lt;/ignoreWarnings&gt;
     *     &lt;warnAll&gt;true|false&lt;/warnAll&gt;
     *     &lt;warningsAsErrors&gt;true|false&lt;/warningsAsErrors&gt;
     *     &lt;config&gt;&lt;/config&gt;
     *     &lt;fullPath&gt;true|false&lt;/fullPath&gt;
     *     &lt;showAll&gt;true|false&lt;/showAll&gt;
     *     &lt;disableSourceLines&gt;true|false&lt;/disableSourceLines&gt;
     *     &lt;url&gt;none|a=b&lt;/url&gt;
     *     &lt;enableHtml&gt;true|false&lt;/enableHtml&gt;
     *     &lt;htmlOutputPath&gt;${project.build.directory}/lint-results/lint-results-html/&lt;/htmlOutputPath&gt;
     *     &lt;enableSimpleHtml&gt;true|false&lt;/enableSimpleHtml&gt;
     *     &lt;simpleHtmlOutputPath&gt;${project.build.directory}/lint-results/lint-results-simple-html
     *     &lt;/simpleHtmlOutputPath&gt;
     *     &lt;enableXml&gt;true|false&lt;/enableXml&gt;
     *     &lt;xmlOutputPath&gt;${project.build.directory}/lint-results/lint-results.xml&lt;/xmlOutputPath&gt;
     *     &lt;enableSources&gt;true|false&lt;/enableSources&gt;
     *     &lt;sources&gt;${project.build.sourceDirecory}}&lt;/sources&gt;
     *     &lt;enableClasspath&gt;true|false&lt;/enableClasspath&gt;
     *     &lt;classpath&gt;${project.build.outputDirectory}&lt;/classpath&gt;
     *     &lt;enableLibraries&gt;true|false&lt;/enableLibraries&gt;
     *     &lt;libraries&gt;&lt;/libraries&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * 
     * Alternatively to the plugin configuration values can also be configured as properties on the command line as 
     * android.lint.* or in pom or settings file as properties like lint*.
     * 
     * @parameter
     */
    @ConfigPojo
    private Lint lint;

    /**
     * Fail build on lint errors. Defaults to "false".
     * 
     * @parameter property="android.lint.failOnError"
     * @see com.jayway.maven.plugins.android.configuration.Lint#failOnError
     */
    private Boolean lintFailOnError;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedFailOnError;

    /**
     * Skip the lint goal execution. Defaults to "true".
     * 
     * @parameter property="android.lint.skip"
     * @see com.jayway.maven.plugins.android.configuration.Lint#skip
     */
    private Boolean lintSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Only check for errors and ignore warnings. Defaults to "false".
     * 
     * @parameter property="android.lint.ignoreWarning"
     * @see com.jayway.maven.plugins.android.configuration.Lint#ignoreWarnings
     */
    private Boolean lintIgnoreWarnings;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedIgnoreWarnings;

    /**
     * Check all warnings, including those off by default. Defaults to "false".
     * 
     * @parameter property="android.lint.warnAll"
     * @see com.jayway.maven.plugins.android.configuration.Lint#warnAll
     */
    private Boolean lintWarnAll;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWarnAll;

    /**
     * Report all warnings as errors. Defaults to "false".
     * 
     * @parameter property="android.lint.warningsAsErrors"
     * @see com.jayway.maven.plugins.android.configuration.Lint#warningsAsErrors
     */
    private Boolean lintWarningsAsErrors;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWarningsAsErrors;

    /**
     * Use the given configuration file to determine whether issues are enabled or disabled. Defaults is "null" so no 
     * config file will be used. To use the commonly used lint.xml in the project root set the parameter to 
     * "${project.basedir}/lint.xml".
     * 
     * @parameter property="android.lint.config"
     * @see com.jayway.maven.plugins.android.configuration.Lint#config
     */
    private String lintConfig;

    @PullParameter( defaultValue = "null" )
    private String parsedConfig;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Use full paths in the error output. Defaults to "false".
     * 
     * @parameter property="android.lint.fullPath"
     * @see com.jayway.maven.plugins.android.configuration.Lint#fullPath
     */
    private Boolean lintFullPath;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedFullPath;

    /**
     * Do not truncate long messages, lists of alternate locations, etc. Defaults to "true".
     * 
     * @parameter property="android.lint.showAll"
     * @see com.jayway.maven.plugins.android.configuration.Lint#showAll
     */
    private Boolean lintShowAll;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedShowAll;

    /**
     * Do not include the source file lines with errors in the output. By default, the error output includes snippets of
     * source code on the line containing the error, but this flag turns it off. Defaults to "false".
     * 
     * @parameter property="android.lint.disableSourceLines"
     * @see com.jayway.maven.plugins.android.configuration.Lint#disableSourceLines
     */
    private Boolean lintDisableSourceLines;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedDisableSourceLines;

    /**
     * Add links to HTML report, replacing local path prefixes with url prefix. The mapping can be a comma-separated
     * list of path prefixes to corresponding URL prefixes, such as C:\temp\Proj1=http://buildserver/sources/temp/Proj1.
     * To turn off linking to files, use --url none. Defaults to "none".
     * 
     * @parameter property="android.lint.url"
     * @see com.jayway.maven.plugins.android.configuration.Lint#url
     */
    private String lintUrl;

    @PullParameter( defaultValue = "none" )
    private String parsedUrl;

    /**
     * Enable the creation of a HTML report. Defaults to "false".
     * 
     * @parameter property="android.lint.enableHtml"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableHtml
     */
    private Boolean lintEnableHtml;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableHtml;

    /**
     * Path for the HTML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. Defaults to ${project.build.directory}/lint/lint-html/.
     * 
     * @parameter property="android.lint.htmlOutputPath"
     * @see com.jayway.maven.plugins.android.configuration.Lint#htmlOutputPath
     */
    private String lintHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getHtmlOutputPath" )
    private String parsedHtmlOutputPath;

    /**
     * Enable the creation of a simple HTML report. Defaults to "false".
     * 
     * @parameter property="android.lint.enableSimpleHtml"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableSimpleHtml
     */
    private Boolean lintEnableSimpleHtml;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableSimpleHtml;

    /**
     * Create a simple HTML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. Defaults to ${project.build.directory}/lint/lint-simple-html/.
     * 
     * @parameter property="android.lint.simpleHtmlOutputPath"
     * @see com.jayway.maven.plugins.android.configuration.Lint#simpleHtml
     */
    private String lintSimpleHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getSimpleHtmlOutputPath" )
    private String parsedSimpleHtmlOutputPath;

    /**
     * Enable the creation of a XML report. Defaults to "true".
     * 
     * @parameter property="android.lint.enableXml"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableXml
     */
    private Boolean lintEnableXml;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedEnableXml;

    /**
     * Create an XML report. If the filename is a directory (or a new filename without an extension), lint will create a
     * separate report for each scanned project. Defaults to ${project.build.directory}/lint/lint-results.xml.
     * 
     * @parameter property="android.lint.xmlOutputPath"
     * @see com.jayway.maven.plugins.android.configuration.Lint#xmlOutputPath
     */
    private String lintXmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getXmlOutputPath" )
    private String parsedXmlOutputPath;

    // ---------------
    // Project Options
    // ---------------

    /**
     * Enable including sources into lint analysis. Defaults to "true".
     * 
     * @parameter property="android.lint.enableSources"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableSources
     */
    private Boolean lintEnableSource;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedEnableSources;

    /**
     * Add the given folder (or path) as a source directory for the project. Only valid when running lint on a single
     * project. Defaults to ${project.build.sourceDirectory}.
     * 
     * @parameter property="android.lint.sources"
     * @see com.jayway.maven.plugins.android.configuration.Lint#sources
     * 
     */
    private String lintSources;

    @PullParameter( defaultValueGetterMethod = "getSources" )
    private String parsedSources;

    /**
     * Enable including classpath into lint analysis. Defaults to "false".
     * 
     * @parameter property="android.lint.enableSources"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableClasspath
     * @see com.jayway.maven.plugins.android.configuration.Lint#classpath
     */
    private Boolean lintEnableClasspath;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableClasspath;

    /**
     * Add the given folder (or jar file, or path) as a class directory for the project. Only valid when running lint on
     * a single project. Defaults to ${project.build.outputDirectory}. Consequently, the lint output depends on the
     * phase during which this goal is executed, whether project has been compiled or not.
     * 
     * @parameter property="android.lint.classpath"
     * @see com.jayway.maven.plugins.android.configuration.Lint#classpath
     * 
     */
    private String lintClasspath;

    @PullParameter( defaultValueGetterMethod = "getClasspath" )
    private String parsedClasspath;

    /**
     * Enable including libraries into lint analysis. Defaults to "false".
     * 
     * @parameter property="android.lint.enableSources"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableLibraries
     * @see com.jayway.maven.plugins.android.configuration.Lint#libraries
     */
    private Boolean lintEnableLibraries;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableLibraries;

    /**
     * Add the given folder (or jar file, or path) as a class library for the project. Only valid when running lint on a
     * single project. Defaults to all non provided resolved artifacts. Consequently, the lint output depends on the
     * phase during which this goal is executed, whether project's dependencies have been resolved or not.
     * 
     * @parameter property="android.lint.libraries"
     * @see com.jayway.maven.plugins.android.configuration.Lint#lintLibraries
     * 
     */
    private String lintLibraries;

    @PullParameter( defaultValueGetterMethod = "getLibraries" )
    private String parsedLibraries;

    /**
     * Execute the mojo by parsing the config and actually invoking the lint command from the Android SDK.
     * 
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();
        getLog().debug( "Parsed values for Android Lint invocation: " );
        getLog().debug( "failOnError:" + parsedFailOnError );
        getLog().debug( "skip:" + parsedSkip );
        getLog().debug( "ignoreWarnings:" + parsedIgnoreWarnings );
        getLog().debug( "warnAll:" + parsedWarnAll );
        getLog().debug( "warningsAsErrors:" + parsedWarningsAsErrors );
        getLog().debug( "config2:" + parsedConfig );

        getLog().debug( "fullPath:" + parsedFullPath );
        getLog().debug( "showAll:" + parsedShowAll );
        getLog().debug( "disableSourceLines:" + parsedDisableSourceLines );

        getLog().debug( "enablehtml: " + parsedEnableHtml );
        getLog().debug( "htmlOutputPath:" + parsedHtmlOutputPath );

        getLog().debug( "enableSimpleHtml: " + parsedEnableSimpleHtml );
        getLog().debug( "simpleHtmlOutputPath:" + parsedSimpleHtmlOutputPath );

        getLog().debug( "enableXml: " + parsedEnableXml );
        getLog().debug( "xmlOutputPath:" + parsedXmlOutputPath );

        getLog().debug( "sources:" + parsedSources );
        getLog().debug( "classpath:" + parsedClasspath );
        getLog().debug( "libraries:" + parsedLibraries );

        if ( parsedSkip )
        {
            getLog().info( "Skipping lint analysis." );
        }
        else
        {
            getLog().info( "Performing lint analysis." );
            executeWhenConfigured();
        }
    }

    private void executeWhenConfigured() throws MojoExecutionException
    {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        String command = getAndroidSdk().getLintPath();

        List< String > parameters = new ArrayList< String >();

        if ( isNotNullAndTrue( parsedIgnoreWarnings ) )
        {
            parameters.add( "-w" );
        }
        if ( isNotNullAndTrue( parsedWarnAll ) )
        {
            parameters.add( "-Wall" );
        }
        if ( isNotNullAndTrue( parsedWarningsAsErrors ) )
        {
            parameters.add( "-Werror" );
        }

        if ( isNotNullAndNotEquals( parsedConfig, "null" ) )
        {
            parameters.add( "--config" );
            parameters.add( parsedConfig );
        }

        if ( isNotNullAndTrue( parsedFullPath ) )
        {
            parameters.add( "--fullpath" );
        }
        if ( isNotNullAndTrue( parsedShowAll ) )
        {
            parameters.add( "--showall" );
        }
        if ( isNotNullAndTrue( parsedDisableSourceLines ) )
        {
            parameters.add( "--nolines" );
        }
        if ( isNotNullAndTrue( parsedEnableHtml ) )
        {
            parameters.add( "--html" );
            parameters.add( parsedHtmlOutputPath );
            getLog().info( "Writing Lint HTML report in " + parsedHtmlOutputPath );
        }
        if ( isNotNullAndNotEquals( parsedUrl, "none" ) )
        {
            parameters.add( "--url" );
            parameters.add( parsedUrl );
        }
        if ( isNotNullAndTrue( parsedEnableSimpleHtml ) )
        {
            parameters.add( "--simplehtml" );
            parameters.add( parsedSimpleHtmlOutputPath );
            getLog().info( "Writing Lint simple HTML report in " + parsedSimpleHtmlOutputPath );
        }
        if ( isNotNullAndTrue( parsedEnableXml ) )
        {
            parameters.add( "--xml" );
            parameters.add( parsedXmlOutputPath );
            getLog().info( "Writing Lint XML report in " + parsedXmlOutputPath );
        }
        if ( isNotNullAndTrue( parsedEnableSources ) )
        {
            parameters.add( "--sources" );
            parameters.add( parsedSources );
        }
        if ( isNotNullAndTrue( parsedEnableClasspath ) )
        {
            parameters.add( "--classpath" );
            parameters.add( parsedClasspath );
        }
        if ( isNotNullAndTrue( parsedEnableLibraries ) )
        {
            parameters.add( "--libraries" );
            parameters.add( parsedLibraries );
        }

        parameters.add( project.getBasedir().getAbsolutePath() );

        // change return code if errors
        // see http://developer.android.com/tools/help/lint.html
        // option not provided by lint --help
        parameters.add( "--exitcode" );
        try
        {
            getLog().info( "Running command: " + command );
            getLog().info( "with parameters: " + parameters );
            executor.executeCommand( command, parameters, false );
        }
        catch ( ExecutionException e )
        {
            if ( isNotNullAndTrue( parsedFailOnError ) )
            {
                getLog().info( "Lint analysis produced errors and project is configured to fail on error." );
                getLog().info( "Inspect lint reports or re-run with -X to see lint errors in log" );
                getLog().info( "Failing build as configured. Ignore following error message." );
                throw new MojoExecutionException( "", e );
            }
        }
        getLog().info( "Lint analysis completed successfully." );
    }

    private boolean isNotNullAndTrue( Boolean b )
    {
        return b != null && b;
    }

    private boolean isNotNullAndNotEquals( String underTest, String compared )
    {
        return underTest != null && !underTest.equals( compared );
    }

    // used via PullParameter annotation - do not remove
    private String getHtmlOutputPath()
    {
        if ( parsedHtmlOutputPath == null )
        {
            File reportPath = new File( project.getBuild().getDirectory(), "lint-results/lint-results-html" );
            createReportDirIfNeeded( reportPath );
            return reportPath.getAbsolutePath();
        }
        return parsedHtmlOutputPath;
    }

    // used via PullParameter annotation - do not remove
    private String getSimpleHtmlOutputPath()
    {
        if ( parsedSimpleHtmlOutputPath == null )
        {
            File reportPath = new File( project.getBuild().getDirectory(), "lint-results/lint-results-simple-html" );
            createReportDirIfNeeded( reportPath );
            return reportPath.getAbsolutePath();
        }
        return parsedSimpleHtmlOutputPath;
    }

    // used via PullParameter annotation - do not remove
    private String getXmlOutputPath()
    {
        getLog().debug( "get parsed xml output path:" + parsedXmlOutputPath );

        if ( parsedXmlOutputPath == null )
        {
            File reportPath = new File( project.getBuild().getDirectory(), "lint-results/lint-results.xml" );
            createReportDirIfNeeded( reportPath );
            return reportPath.getAbsolutePath();
        }
        return parsedXmlOutputPath;
    }

    private void createReportDirIfNeeded( File reportPath )
    {
        if ( !reportPath.getParentFile().exists() )
        {
            reportPath.getParentFile().mkdirs();
        }
    }

    // used via PullParameter annotation - do not remove
    private String getSources()
    {
        if ( parsedSources == null )
        {
            parsedSources = new File( project.getBuild().getSourceDirectory() ).getAbsolutePath();
        }
        return parsedSources;
    }

    // used via PullParameter annotation - do not remove
    private String getClasspath()
    {
        if ( parsedClasspath == null )
        {
            parsedClasspath = new File( project.getBuild().getOutputDirectory() ).getAbsolutePath();
        }
        return parsedClasspath;
    }

    private String getLibraries()
    {
        if ( parsedLibraries == null )
        {
            StringBuilder defaultClasspathBuilder = new StringBuilder();
            Set< Artifact > artifacts = project.getDependencyArtifacts();
            if ( artifacts != null )
            {
                for ( Artifact artifact : artifacts )
                {
                    if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) && artifact.isResolved() )
                    {
                        defaultClasspathBuilder.append( artifact.getFile().getPath() );
                        defaultClasspathBuilder.append( File.pathSeparator );
                    }
                }
                if ( defaultClasspathBuilder.length() > 0 )
                {
                    defaultClasspathBuilder.deleteCharAt( defaultClasspathBuilder.length() - 1 );
                    parsedLibraries = defaultClasspathBuilder.toString();
                }
            }
        }
        return parsedLibraries;
    }
}
