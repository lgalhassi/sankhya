package br.com.sankhya.ctba.campanhaCartao;

import java.math.BigDecimal;
import java.util.List;

public class DadosCampanha {
	
	BigDecimal codparc;
	BigDecimal valor;
	String     cgcCpf;
	BigDecimal rolCartao;
	String     razaoSocial;
	
	public BigDecimal getCodparc() {
		return codparc;
	}
	public void setCodparc(BigDecimal codparc) {
		this.codparc = codparc;
	}
	public BigDecimal getValor() {
		return valor;
	}
	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}
	public String getCgcCpf() {
		return cgcCpf;
	}
	public void setCgcCpf(String cgcCpf) {
		this.cgcCpf = cgcCpf;
	}
	public BigDecimal getRolCartao() {
		return rolCartao;
	}
	public void setRolCartao(BigDecimal rolCartao) {
		this.rolCartao = rolCartao;
	}
	public String getRazaoSocial() {
		return razaoSocial;
	}
	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}
	
	
	

}
