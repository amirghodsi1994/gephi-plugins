/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ixxi.bridginess;

import org.ixxi.bridginess.Bridginess;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author matteo
 */

@ServiceProvider(service = StatisticsBuilder.class)
public class BridginessBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
        return "Bridginess";
    }

    @Override
    public Statistics getStatistics() {
        //throw new UnsupportedOperationException("Not supported yet.");
        return new Bridginess();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return Bridginess.class;
    }

}
