/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ixxi.stirling;

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
public class StirlingUI implements StatisticsUI {

    private StirlingPanel panel;
    private Stirling stirling;

    @Override
    public JPanel getSettingsPanel() {
        panel = new StirlingPanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistics) {
        this.stirling = (Stirling) statistics;
        if (panel != null) {
        }
    }

    @Override
    public void unsetup() {
        if (panel != null) {
         }
        stirling = null;
        panel = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return Stirling.class;
    }

    @Override
    public String getValue() {
        //return "42";
        DecimalFormat df = new DecimalFormat("###.##");
        return df.format(stirling.getAvgBridginess()).toString();
    }

    @Override
    public String getDisplayName() {
        return "Stirling";
    }

    @Override
    public String getShortDescription() {
        return "Stirling implementation (test)";
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
