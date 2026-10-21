package com.fitnutri.backend.common;
import java.sql.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SchemaReadinessGateTest {
    DataSource data;Connection connection;Statement statement;ResultSet rows;
    @BeforeEach void arrange()throws Exception{data=mock(DataSource.class);connection=mock(Connection.class);statement=mock(Statement.class);rows=mock(ResultSet.class);when(data.getConnection()).thenReturn(connection);when(connection.createStatement()).thenReturn(statement);when(statement.executeQuery(anyString())).thenReturn(rows);}
    private void approvedHistory() throws Exception {
        var values=java.util.stream.IntStream.rangeClosed(1,SchemaReadinessGate.APPROVED_VERSION).mapToObj(Integer::toString).iterator();
        when(rows.next()).thenAnswer(i->values.hasNext());when(rows.getString(1)).thenAnswer(i->values.next());when(rows.getBoolean(2)).thenReturn(true);
    }
    @Test void approvedHistoryIsReadOnlyAndEveryJdbcResourceCloses()throws Exception{approvedHistory();assertDoesNotThrow(()->new SchemaReadinessGate(data));verify(statement,never()).executeUpdate(anyString());verify(connection,never()).commit();verify(rows).close();verify(statement).close();verify(connection).close();}
    @Test void olderAndUnknownFutureHistoryAreRejectedInsteadOfAssumingColumnValidationMeansMigration()throws Exception{
        when(rows.next()).thenReturn(true,false);when(rows.getString(1)).thenReturn("4");when(rows.getBoolean(2)).thenReturn(true);assertThrows(IllegalStateException.class,()->new SchemaReadinessGate(data));
        when(rows.next()).thenReturn(true,false);when(rows.getString(1)).thenReturn(Integer.toString(SchemaReadinessGate.APPROVED_VERSION+1));assertThrows(IllegalStateException.class,()->new SchemaReadinessGate(data));
    }
    @Test void unsuccessfulMigrationIsNotReady()throws Exception{when(rows.next()).thenReturn(true,false);when(rows.getString(1)).thenReturn("8");when(rows.getBoolean(2)).thenReturn(false);assertThrows(IllegalStateException.class,()->new SchemaReadinessGate(data));}
    @Test void missingHistoryFailsSafelyWithoutLeakingSqlConnectionOrCredentialDetails()throws Exception{when(statement.executeQuery(anyString())).thenThrow(new SQLException("synthetic-private-password-url"));var failure=assertThrows(IllegalStateException.class,()->new SchemaReadinessGate(data));assertFalse(failure.toString().contains("synthetic-private"));assertNull(failure.getCause());verify(statement).close();verify(connection).close();}
    @Test void preValidationCustomizerPreservesHibernateValidationAndOnlyReadsHistory()throws Exception {
        approvedHistory();
        var properties=new java.util.HashMap<String,Object>();properties.put("hibernate.hbm2ddl.auto","validate");
        new SchemaReadinessCustomizer(data).customize(properties);
        assertEquals(java.util.Map.of("hibernate.hbm2ddl.auto","validate"),properties);
        verify(statement).executeQuery("SELECT version,success FROM public.flyway_schema_history");
        verify(statement,never()).executeUpdate(anyString());verify(connection,never()).commit();
    }
}
