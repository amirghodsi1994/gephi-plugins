/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ixxi.stirling;

import org.ixxi.stirling.Stirling;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author matteo
 */

@ServiceProvider(service = StatisticsBuilder.class)
public class StirlingBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
        return "Stirling";
    }

    @Override
    public Statistics getStatistics() {
        //throw new UnsupportedOperationException("Not supported yet.");
        return new Stirling();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return Stirling.class;
    }

}
