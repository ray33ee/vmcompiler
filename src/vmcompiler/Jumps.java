/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmcompiler;

/**
 *
 * @author Will
 */
public class Jumps {
    
    public int _position;
    
    public String _label;
    
    public Jumps(int pos, String label)
    {
        this._position = pos;
        this._label = label;
    }
    
    @Override
    public String toString()
    {
        return "{" + this._position + ", " + this._label + "}";
    }
    
}
