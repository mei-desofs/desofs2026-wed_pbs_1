# SpotBugs e SAST

## Objectivo

SpotBugs é usado como sinal SAST em conjunto com CodeQL e SonarCloud. Detecta
padrões Java suspeitos e fornece evidência revível, mas não substitui revisão
manual nem testes de segurança.

## Comando local

```powershell
cd ghostreport
.\mvnw.cmd -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs "-Dspotbugs.xmlOutput=true"
```

A forma com `"-Dspotbugs.xmlOutput=true"` evita problemas de parsing no
PowerShell.

## Evidência de pipeline

O job `sast` em `.github/workflows/dev.yml` corre:

- CodeQL Java;
- SpotBugs em modo de evidência;
- SonarCloud quando o token necessário está configurado.

O artefacto `sast-reports` inclui XML/site output do SpotBugs quando gerado.

## Regras de triagem

| Tipo de finding | Acção |
| --- | --- |
| Bug de segurança confirmado | Corrigir em código e adicionar/actualizar testes. |
| Falso positivo | Documentar regra, ficheiro e motivo. |
| Risco teórico sem exploração no âmbito | Documentar como risco residual ou hardening futuro. |
| Problema de configuração | Corrigir workflow/plugin antes de usar o resultado como evidência. |

Uma execução sem findings não prova que a aplicação é segura; é apenas evidência
complementar.
