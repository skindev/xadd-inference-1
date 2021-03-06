package hgm.poly.reports.sg.external.stan;

import hgm.poly.sampling.SamplerInterface;

/**
 * Created by Hadi Afshar.
 * Date: 18/01/15
 * Time: 2:41 AM
 *
 * The samples are internal i.e. as generated by Stan therefore the number and order of variables may not match that of the factory
 */
public interface LowLevelStanSamplerInterface extends SamplerInterface{
    /**
     *
     * @return variables in the order of which low-level samples are taken
     */
    String[] getStanOutputSampleVars();
}
