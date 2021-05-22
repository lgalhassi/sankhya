package br.com.sankhya.ctba.LembreteTituloVencido;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class TituloVencido {
	BigDecimal codparc;
    String nomeparc;
    String cgc_cpf;
    String cgc_cpf_mask;
    BigDecimal numnota; 
    Timestamp dtneg;
    BigDecimal sequencia; 
    Timestamp dtvenc;
    String vlrdesdo;
    String email;
    String nomeFantasiaEmpr;
    String emailEmpr;
    String foneEmpr;
    
	public BigDecimal getCodparc() {
		return codparc;
	}
	public void setCodparc(BigDecimal codparc) {
		this.codparc = codparc;
	}
	public String getNomeparc() {
		return nomeparc;
	}
	public void setNomeparc(String nomeparc) {
		this.nomeparc = nomeparc;
	}
	public String getCgc_cpf() {
		return cgc_cpf;
	}
	public void setCgc_cpf(String cgc_cpf) {
		this.cgc_cpf = cgc_cpf;
	}
	
	public String getCgc_cpf_mask() {
		return cgc_cpf_mask;
	}
	public void setCgc_cpf_mask(String cgc_cpf_mask) {
		this.cgc_cpf_mask = cgc_cpf_mask;
	}
	public BigDecimal getNumnota() {
		return numnota;
	}
	public void setNumnota(BigDecimal numnota) {
		this.numnota = numnota;
	}
	public Timestamp getDtneg() {
		return dtneg;
	}
	public void setDtneg(Timestamp dtneg) {
		this.dtneg = dtneg;
	}
	public BigDecimal getSequencia() {
		return sequencia;
	}
	public void setSequencia(BigDecimal sequencia) {
		this.sequencia = sequencia;
	}
	public Timestamp getDtvenc() {
		return dtvenc;
	}
	public void setDtvenc(Timestamp dtvenc) {
		this.dtvenc = dtvenc;
	}
	public String getVlrdesdo() {
		return vlrdesdo;
	}
	public void setVlrdesdo(String vlrdesdo) {
		this.vlrdesdo = vlrdesdo;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNomeFantasiaEmpr() {
		return nomeFantasiaEmpr;
	}
	public void setNomeFantasiaEmpr(String nomeFantasiaEmpr) {
		this.nomeFantasiaEmpr = nomeFantasiaEmpr;
	}
	public String getEmailEmpr() {
		return emailEmpr;
	}
	public void setEmailEmpr(String emailEmpr) {
		this.emailEmpr = emailEmpr;
	}
	public String getFoneEmpr() {
		return foneEmpr;
	}
	public void setFoneEmpr(String foneEmpr) {
		this.foneEmpr = foneEmpr;
	}
    
	
    

}
