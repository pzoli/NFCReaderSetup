/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.infokristaly.nfcreader.guisetup;

/**
 *
 * @author pzoli
 */
class ReaderConfig {
    public byte useDHCP = 1;
    public byte[] serverip = {(byte) 192, (byte) 168, (byte) 1, (byte) 67};
    public byte[] ip = {(byte) 192, (byte) 168, (byte) 1, (byte) 177};
    public byte[] subnet = {(byte) 255, (byte) 255, (byte) 255, (byte) 0};
    public byte[] dnsserver = {(byte) 192, (byte) 168, (byte) 1, (byte) 1};
    public byte[] gateway = {(byte) 192, (byte) 168, (byte) 1, (byte) 1};    
}
