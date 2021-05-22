package br.com.sankhya.ctba.contrato;

import java.math.BigDecimal;

public class Funcionario {

	private BigDecimal codfunc;
	private BigDecimal codemp;
	private BigDecimal codcargahor;
	private BigDecimal codfuncao;
	private BigDecimal codserv;
	private BigDecimal coddep;
	
	public BigDecimal getCoddep() {
		return coddep;
	}
	public void setCoddep(BigDecimal coddep) {
		this.coddep = coddep;
	}
	public BigDecimal getCodserv() {
		return codserv;
	}
	public void setCodserv(BigDecimal codserv) {
		this.codserv = codserv;
	}
	public BigDecimal getCodfuncao() {
		return codfuncao;
	}
	public void setCodfuncao(BigDecimal codfuncao) {
		this.codfuncao = codfuncao;
	}
	public BigDecimal getCodfunc() {
		return codfunc;
	}
	public void setCodfunc(BigDecimal codfunc) {
		this.codfunc = codfunc;
	}
	public BigDecimal getCodemp() {
		return codemp;
	}
	public void setCodemp(BigDecimal codemp) {
		this.codemp = codemp;
	}
	public BigDecimal getCodcargahor() {
		return codcargahor;
	}
	public void setCodcargahor(BigDecimal codcargahor) {
		this.codcargahor = codcargahor;
	}
	
}
