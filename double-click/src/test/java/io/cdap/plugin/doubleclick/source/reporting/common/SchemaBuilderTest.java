package io.cdap.plugin.doubleclick.source.reporting.common;

import io.cdap.cdap.api.data.schema.Schema;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SchemaBuilderTest {

  @Test
  public void testBuildSchemaCaseNotEmpty() {
    //given
    String givenMetric1 = "dfa:users";
    String givenMetric2 = "dfa:dcmCost";
    String givenDimension1 = "dfa:userType";
    String givenDimension2 = "dfa:sessionCount";
    List<String> givenMetrics = Arrays.asList(givenMetric1, givenMetric2);
    List<String> givenDimensions = Arrays.asList(givenDimension1, givenDimension2);

    //when
    Schema schema = SchemaBuilder.buildSchema(givenMetrics, givenDimensions);

    //then
    Assert.assertNotNull(schema);
    Assert.assertEquals(givenMetric1, Objects.requireNonNull(schema.getField(givenMetric1)).getName());
    Assert.assertEquals(givenMetric2, Objects.requireNonNull(schema.getField(givenMetric2)).getName());
    Assert.assertEquals(givenDimension1, Objects.requireNonNull(schema.getField(givenDimension1)).getName());
    Assert.assertEquals(givenDimension2, Objects.requireNonNull(schema.getField(givenDimension2)).getName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildSchemaCaseEmpty() {
    //when
    SchemaBuilder.buildSchema(new ArrayList<>(), new ArrayList<>());
  }

}
