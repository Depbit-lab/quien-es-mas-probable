package com.depbit.sinfiltro;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

public final class NostrCrypto {
    private static final BigInteger P = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",16);
    private static final BigInteger N = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141",16);
    private static final BigInteger GX = new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798",16);
    private static final BigInteger GY = new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",16);
    private static final Point G = new Point(GX,GY);
    private NostrCrypto() {}

    static final class Point {
        final BigInteger x,y; final boolean infinity;
        Point(BigInteger x,BigInteger y){this.x=x;this.y=y;this.infinity=false;}
        Point(){this.x=BigInteger.ZERO;this.y=BigInteger.ZERO;this.infinity=true;}
    }

    public static boolean isValidSecret(byte[] sk){ if(sk==null||sk.length!=32)return false;BigInteger d=new BigInteger(1,sk);return d.signum()>0&&d.compareTo(N)<0; }
    public static String getPublicKeyHex(byte[] sk){ BigInteger d=new BigInteger(1,sk);Point p=multiply(G,d);return hex(bytes32(p.x)); }
    public static byte[] sha256(byte[] data) throws Exception{return MessageDigest.getInstance("SHA-256").digest(data);}

    public static byte[] schnorrSign(byte[] message, byte[] secret, byte[] aux) throws Exception {
        if(message.length!=32||!isValidSecret(secret)||aux.length!=32)throw new IllegalArgumentException();
        BigInteger d0=new BigInteger(1,secret); Point pub=multiply(G,d0); BigInteger d=isEven(pub.y)?d0:N.subtract(d0);
        byte[] t=xor(bytes32(d),taggedHash("BIP0340/aux",aux));
        byte[] nonceInput=concat(t,bytes32(pub.x),message);
        BigInteger k0=new BigInteger(1,taggedHash("BIP0340/nonce",nonceInput)).mod(N);
        if(k0.signum()==0)throw new IllegalStateException("nonce");
        Point rPoint=multiply(G,k0); BigInteger k=isEven(rPoint.y)?k0:N.subtract(k0);
        BigInteger e=new BigInteger(1,taggedHash("BIP0340/challenge",concat(bytes32(rPoint.x),bytes32(pub.x),message))).mod(N);
        BigInteger s=k.add(e.multiply(d)).mod(N);
        byte[] sig=concat(bytes32(rPoint.x),bytes32(s));
        if(!schnorrVerify(message,bytes32(pub.x),sig))throw new IllegalStateException("self verify");
        return sig;
    }

    public static boolean schnorrVerify(byte[] message, byte[] publicKey, byte[] signature) {
        try {
            if(message.length!=32||publicKey.length!=32||signature.length!=64)return false;
            BigInteger px=new BigInteger(1,publicKey); Point pub=liftX(px); if(pub==null)return false;
            BigInteger r=new BigInteger(1,Arrays.copyOfRange(signature,0,32)); BigInteger s=new BigInteger(1,Arrays.copyOfRange(signature,32,64));
            if(r.compareTo(P)>=0||s.compareTo(N)>=0)return false;
            BigInteger e=new BigInteger(1,taggedHash("BIP0340/challenge",concat(bytes32(r),bytes32(pub.x),message))).mod(N);
            Point result=add(multiply(G,s),multiply(pub,N.subtract(e)));
            return !result.infinity&&isEven(result.y)&&result.x.equals(r);
        }catch(Exception e){return false;}
    }

    private static Point liftX(BigInteger x){
        if(x.signum()<0||x.compareTo(P)>=0)return null;
        BigInteger c=x.modPow(BigInteger.valueOf(3),P).add(BigInteger.valueOf(7)).mod(P);
        BigInteger y=c.modPow(P.add(BigInteger.ONE).shiftRight(2),P);
        if(!y.multiply(y).mod(P).equals(c))return null;
        if(!isEven(y))y=P.subtract(y); return new Point(x,y);
    }
    private static boolean isEven(BigInteger x){return !x.testBit(0);}
    private static Point add(Point a,Point b){
        if(a.infinity)return b;if(b.infinity)return a;
        if(a.x.equals(b.x)){if(!a.y.equals(b.y)||a.y.signum()==0)return new Point();return doublePoint(a);}
        BigInteger slope=b.y.subtract(a.y).multiply(b.x.subtract(a.x).mod(P).modInverse(P)).mod(P);
        BigInteger x=slope.multiply(slope).subtract(a.x).subtract(b.x).mod(P);
        BigInteger y=slope.multiply(a.x.subtract(x)).subtract(a.y).mod(P);return new Point(x,y);
    }
    private static Point doublePoint(Point a){
        if(a.infinity||a.y.signum()==0)return new Point();
        BigInteger slope=a.x.multiply(a.x).multiply(BigInteger.valueOf(3)).multiply(a.y.shiftLeft(1).mod(P).modInverse(P)).mod(P);
        BigInteger x=slope.multiply(slope).subtract(a.x.shiftLeft(1)).mod(P);
        BigInteger y=slope.multiply(a.x.subtract(x)).subtract(a.y).mod(P);return new Point(x,y);
    }
    private static Point multiply(Point point,BigInteger scalar){
        Point result=new Point(),addend=point;BigInteger k=scalar.mod(N);
        while(k.signum()>0){if(k.testBit(0))result=add(result,addend);addend=doublePoint(addend);k=k.shiftRight(1);}return result;
    }
    private static byte[] taggedHash(String tag,byte[] message)throws Exception{byte[] h=sha256(tag.getBytes("UTF-8"));return sha256(concat(h,h,message));}
    private static byte[] xor(byte[] a,byte[] b){byte[] r=new byte[a.length];for(int i=0;i<a.length;i++)r[i]=(byte)(a[i]^b[i]);return r;}
    private static byte[] concat(byte[]... arrays){int n=0;for(byte[]a:arrays)n+=a.length;byte[]r=new byte[n];int p=0;for(byte[]a:arrays){System.arraycopy(a,0,r,p,a.length);p+=a.length;}return r;}
    private static byte[] bytes32(BigInteger value){byte[] raw=value.toByteArray();byte[] out=new byte[32];int src=Math.max(0,raw.length-32),len=Math.min(32,raw.length);System.arraycopy(raw,src,out,32-len,len);return out;}
    public static String hex(byte[] bytes){StringBuilder s=new StringBuilder(bytes.length*2);for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
    public static byte[] unhex(String hex){if(hex.length()%2!=0)throw new IllegalArgumentException();byte[]out=new byte[hex.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);return out;}
}
