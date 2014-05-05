/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ixxi.bridginess;

import java.text.DecimalFormat;
import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author matteo
 */
@ServiceProvider(service = StatisticsUI.class)
public class BridginessUI implements StatisticsUI {

    private BridginessPanel panel;
    private Bridginess bridginess;

    @Override
    public JPanel getSettingsPanel() {
        panel = new BridginessPanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistics) {
        this.bridginess = (Bridginess) statistics;
        if (panel != null) {
            panel.setExclNeighbors(bridginess.getExclNeighbors());
        }
    }

    @Override
    public void unsetup() {
        if (panel != null) {
            bridginess.setExclNeighbors(panel.getExclNeighbors());
        }
        bridginess = null;
        panel = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return Bridginess.class;
    }

    @Override
    public String getValue() {
        //return "42";
        DecimalFormat df = new DecimalFormat("###.##");
        return df.format(bridginess.getAvgBridginess()).toString();
    }

    @Override
    public String getDisplayName() {
        return "Bridginess";
    }

    @Override
    public String getShortDescription() {
        return "Bridginess implementation (test)";
    }

    @Override
    public String getCategory() {
        return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
    }

    @Override
    public int getPosition() {
        return 1;
    }

}
