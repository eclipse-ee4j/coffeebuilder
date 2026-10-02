package org.eclipse.coffeebuilder.mojo.persistence;

import org.eclipse.coffeebuilder.helper.JakartaEeHelper;
import org.eclipse.coffeebuilder.helper.PersistenceXmlHelper;
import org.eclipse.coffeebuilder.util.CoffeeBuilderUtil;
import org.eclipse.coffeebuilder.util.MavenProjectUtil;
import org.eclipse.coffeebuilder.util.PomUtil;
import jakarta.json.Json;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddDataSourceMojoTest {

    @Mock
    private MavenProject mavenProject;

    @Mock
    private MavenSession mavenSession;

    @Mock
    private ProjectBuilder projectBuilder;

    @Mock
    private Log mockLog;

    @InjectMocks
    private AddDataSourceMojo mojo;

    private MockedStatic<MavenProjectUtil> mavenProjectUtilMockedStatic;
    private MockedStatic<PomUtil> pomUtilMockedStatic;
    private MockedStatic<JakartaEeHelper> jakartaEeHelperMockedStatic;
    private MockedStatic<PersistenceXmlHelper> persistenceXmlHelperMockedStatic;
    private MockedStatic<CoffeeBuilderUtil> coffeeBuilderUtilMockedStatic;

    @Mock
    private JakartaEeHelper jakartaEeHelperMock;

    @Mock
    private PersistenceXmlHelper persistenceXmlHelperMock;

    @BeforeEach
    void setUp() throws Exception {
        mojo.setLog(mockLog);

        mavenProjectUtilMockedStatic = mockStatic(MavenProjectUtil.class);
        pomUtilMockedStatic = mockStatic(PomUtil.class);
        jakartaEeHelperMockedStatic = mockStatic(JakartaEeHelper.class);
        persistenceXmlHelperMockedStatic = mockStatic(PersistenceXmlHelper.class);
        coffeeBuilderUtilMockedStatic = mockStatic(CoffeeBuilderUtil.class);

        jakartaEeHelperMockedStatic.when(JakartaEeHelper::getInstance).thenReturn(jakartaEeHelperMock);
        persistenceXmlHelperMockedStatic.when(PersistenceXmlHelper::getInstance).thenReturn(persistenceXmlHelperMock);

        setField("datasourceName", "myDS");
        setField("url", "jdbc:h2:mem:test");
        setField("declare", "web");
        setField("persistenceUnitName", "myPU");
        setField("mavenProject", mavenProject);
        setField("mavenSession", mavenSession);
        setField("projectBuilder", projectBuilder);
    }

    private void setField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = AddAbstractPersistenceMojo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
    }

    @AfterEach
    void tearDown() {
        mavenProjectUtilMockedStatic.close();
        pomUtilMockedStatic.close();
        jakartaEeHelperMockedStatic.close();
        persistenceXmlHelperMockedStatic.close();
        coffeeBuilderUtilMockedStatic.close();
    }

    @Test
    @DisplayName("execute: should add data source successfully")
    void execute_ValidProject_AddsDataSource() throws Exception {
        MavenProject fullProject = mock(MavenProject.class);

        mavenProjectUtilMockedStatic.when(() -> MavenProjectUtil.getFullProject(mavenSession, projectBuilder, mavenProject))
                .thenReturn(fullProject);

        var jdbcConfig = Json.createObjectBuilder().add("dataSourceClass", "org.h2.jdbcx.JdbcDataSource").build();
        coffeeBuilderUtilMockedStatic.when(() -> CoffeeBuilderUtil.getJdbcConfiguration(mockLog,"jdbc:h2:mem:test"))
                .thenReturn(Optional.of(jdbcConfig));

        pomUtilMockedStatic.when(() -> PomUtil.saveMavenProject(fullProject, mockLog)).thenAnswer(inv -> null);

        mojo.execute();

        verify(persistenceXmlHelperMock).addDataSourceToPersistenceXml(eq(fullProject), eq(mockLog), eq("myPU"), eq("jdbc/myDS"));
        verify(jakartaEeHelperMock).checkDataDependencies(eq(fullProject), eq(mockLog), eq(jdbcConfig));
        verify(jakartaEeHelperMock).addDataSource(eq(fullProject), eq(mockLog), eq("web"), any(), any());
        pomUtilMockedStatic.verify(() -> PomUtil.saveMavenProject(fullProject, mockLog));
    }

    @Test
    @DisplayName("execute: should fail when the JDBC dependency cannot be added")
    void execute_JdbcDependencyFailure_FailsGoal() throws Exception {
        MavenProject fullProject = mock(MavenProject.class);
        mavenProjectUtilMockedStatic.when(() -> MavenProjectUtil.getFullProject(mavenSession,
                projectBuilder,
                mavenProject))
            .thenReturn(fullProject);
        var jdbcConfig = Json.createObjectBuilder()
            .add("coordinates", "org.postgresql:postgresql")
            .add("dataSourceClass", "org.postgresql.ds.PGPoolingDataSource")
            .build();
        coffeeBuilderUtilMockedStatic.when(() -> CoffeeBuilderUtil.getJdbcConfiguration(mockLog,
                "jdbc:h2:mem:test"))
            .thenReturn(Optional.of(jdbcConfig));
        org.mockito.Mockito.doThrow(new MojoExecutionException("metadata resolution failed"))
            .when(jakartaEeHelperMock).checkDataDependencies(fullProject, mockLog, jdbcConfig);

        MojoExecutionException exception = org.junit.jupiter.api.Assertions.assertThrows(
            MojoExecutionException.class, mojo::execute);

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("metadata resolution failed"));
        verify(jakartaEeHelperMock, never()).addDataSource(eq(fullProject), eq(mockLog), any(), any(), any());
        pomUtilMockedStatic.verify(() -> PomUtil.saveMavenProject(fullProject, mockLog), never());
    }

    @Test
    @DisplayName("execute: should fail when class datasource generation fails")
    void execute_ClassDatasourceRenderingFailure_FailsGoal() throws Exception {
        MavenProject fullProject = mock(MavenProject.class);
        mavenProjectUtilMockedStatic.when(() -> MavenProjectUtil.getFullProject(mavenSession,
                projectBuilder,
                mavenProject))
            .thenReturn(fullProject);
        var jdbcConfig = Json.createObjectBuilder()
            .add("coordinates", "com.h2database:h2")
            .add("version", "2.4.240")
            .add("dataSourceClass", "org.h2.jdbcx.JdbcDataSource")
            .build();
        coffeeBuilderUtilMockedStatic.when(() -> CoffeeBuilderUtil.getJdbcConfiguration(mockLog,
                "jdbc:h2:mem:test"))
            .thenReturn(Optional.of(jdbcConfig));
        setField("declare", "class");
        var renderingFailure = new java.io.IOException("FreeMarker rendering failed");
        org.mockito.Mockito.doThrow(new MojoExecutionException("Error creating datasource", renderingFailure))
            .when(jakartaEeHelperMock).addDataSource(eq(fullProject), eq(mockLog),
                eq("class"), any(), any());

        MojoExecutionException exception = org.junit.jupiter.api.Assertions.assertThrows(
            MojoExecutionException.class, mojo::execute);

        org.junit.jupiter.api.Assertions.assertSame(renderingFailure, exception.getCause());
        pomUtilMockedStatic.verify(() -> PomUtil.saveMavenProject(fullProject, mockLog), never());
    }
}
