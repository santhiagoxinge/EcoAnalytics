import { ArrowDownRight, ArrowUpRight, Download, GitCompareArrows } from 'lucide-react'
import { useEffect, useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { Button, Callout, EmptyState, Field, PageHeader, Panel, Skeleton } from '../components/ui'
import { api, ApiError, qs } from '../lib/api'
import { daysAgo, fmtDate, int, num, signedPercent, variableLabel } from '../lib/format'
import type { Comparativa, Variable } from '../lib/types'

function shiftYear(iso: string, years: number) {
  const [y, m, d] = iso.split('-')
  return `${Number(y) + years}-${m}-${d}`
}

export default function ComparisonPage() {
  const station = useEffectiveStation()
  const [variable, setVariable] = useState<Variable>('PM25')
  const [bFrom, setBFrom] = useState(daysAgo(30))
  const [bTo, setBTo] = useState(daysAgo(1))
  const [aFrom, setAFrom] = useState(shiftYear(daysAgo(30), -1))
  const [aTo, setATo] = useState(shiftYear(daysAgo(1), -1))
  const [result, setResult] = useState<Comparativa | null>(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)

  const params = () => ({ estacionId: station?.id, variable, aDesde: aFrom, aHasta: aTo, bDesde: bFrom, bHasta: bTo })

  const compare = async () => {
    if (!station) return
    setLoading(true)
    try {
      setResult(await api.get<Comparativa>(`/comparativa?${qs(params())}`))
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setLoading(false)
    }
  }

  const exportReport = async () => {
    setExporting(true)
    try {
      toast.success('Informe comparativo exportado', { description: await api.download(`/comparativa/exportar?${qs(params())}`) })
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setExporting(false)
    }
  }

  // HU-08: the view opens with the default periods already compared.
  useEffect(() => {
    if (station && !result) void compare()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [station?.id])

  const r = result
  return (
    <>
      <PageHeader
        title="Comparativa de periodos"
        subtitle="Compare una variable entre dos intervalos para evaluar medidas de mitigación o variaciones estacionales."
        actions={r?.comparable && <Button variant="secondary" icon={<Download size={16} />} loading={exporting} onClick={exportReport}>Exportar informe</Button>}
      />

      <Panel>
        <div className="filters">
          <Field label="Estación"><StationSelect /></Field>
          <Field label="Variable" htmlFor="cmp-var">
            <select id="cmp-var" className="select" value={variable} onChange={(e) => setVariable(e.target.value as Variable)}>
              {(Object.keys(variableLabel) as Variable[]).map((v) => <option key={v} value={v}>{variableLabel[v]}</option>)}
            </select>
          </Field>
          <fieldset className="period period-a">
            <legend>Periodo A</legend>
            <input type="date" className="input" value={aFrom} max={aTo} onChange={(e) => setAFrom(e.target.value)} aria-label="Inicio del periodo A" />
            <input type="date" className="input" value={aTo} min={aFrom} onChange={(e) => setATo(e.target.value)} aria-label="Fin del periodo A" />
          </fieldset>
          <fieldset className="period period-b">
            <legend>Periodo B</legend>
            <input type="date" className="input" value={bFrom} max={bTo} onChange={(e) => setBFrom(e.target.value)} aria-label="Inicio del periodo B" />
            <input type="date" className="input" value={bTo} min={bFrom} onChange={(e) => setBTo(e.target.value)} aria-label="Fin del periodo B" />
          </fieldset>
          <Button variant="primary" icon={<GitCompareArrows size={16} />} loading={loading} onClick={compare}>Comparar</Button>
        </div>
      </Panel>

      {!r ? (
        loading ? <Skeleton height={360} radius={14} /> : (
          <Panel>
            <EmptyState icon={<GitCompareArrows size={28} />} title="Defina dos periodos y presione Comparar">
              Por defecto se compara el último mes frente al mismo intervalo del año anterior.
            </EmptyState>
          </Panel>
        )
      ) : (
        <>
          {!r.comparable && <Callout tone="warning" title={r.advertencia ?? 'Datos insuficientes'}>No se calculan variaciones para evitar conclusiones erróneas.</Callout>}

          <div className="kpi-row">
            <DeltaCard label={`Promedio ${variableLabel[r.variable]}`} a={r.periodoA.media} b={r.periodoB.media} change={r.variaciones?.cambioMedia} unit={r.unidad} />
            <DeltaCard label="Máximo registrado" a={r.periodoA.maximo} b={r.periodoB.maximo} change={r.variaciones?.cambioMaximo} unit={r.unidad} />
            <DeltaCard label="Mínimo registrado" a={r.periodoA.minimo} b={r.periodoB.minimo} change={r.variaciones?.cambioMinimo} unit={r.unidad} />
            {r.periodoA.diasExcedencia !== null && (
              <div className="panel kpi">
                <p className="stat-label">Días sobre la guía OMS</p>
                <p className="stat-value">{r.periodoA.diasExcedencia} → {r.periodoB.diasExcedencia}</p>
                {r.variaciones && <Change value={r.variaciones.cambioDiasExcedencia} suffix=" días" />}
              </div>
            )}
          </div>

          <Panel
            title={`Superposición diaria · ${r.estacionNombre}`}
            caption={`A: ${fmtDate(r.periodoA.desde)} – ${fmtDate(r.periodoA.hasta)} (${int(r.periodoA.mediciones)} mediciones, cobertura ${Math.round(r.periodoA.cobertura * 100)} %) · B: ${fmtDate(r.periodoB.desde)} – ${fmtDate(r.periodoB.hasta)} (${int(r.periodoB.mediciones)} mediciones, cobertura ${Math.round(r.periodoB.cobertura * 100)} %)`}
            actions={<div className="legend">
              <span className="legend-chip"><span className="dot" style={{ background: 'var(--series-1)' }} /> Periodo A</span>
              <span className="legend-chip"><span className="dot" style={{ background: 'var(--series-4)' }} /> Periodo B</span>
            </div>}
          >
            <div style={{ height: 320 }}>
              <ResponsiveContainer>
                <LineChart data={r.serie} margin={{ top: 8, right: 8, bottom: 0, left: -14 }}>
                  <CartesianGrid vertical={false} stroke="var(--grid)" />
                  <XAxis dataKey="dia" tickFormatter={(d) => `Día ${d}`} minTickGap={30} {...axisProps} />
                  <YAxis width={48} {...axisProps} />
                  <Tooltip content={<ChartTooltip labelFormatter={(l) => `Día ${l}`} units={{ periodoA: r.unidad, periodoB: r.unidad }} />} />
                  <Line dataKey="periodoA" name="Periodo A" stroke="var(--series-1)" strokeWidth={2.4} dot={false} connectNulls animationDuration={700} />
                  <Line dataKey="periodoB" name="Periodo B" stroke="var(--series-4)" strokeWidth={2.4} strokeDasharray="6 4" dot={false} connectNulls animationDuration={700} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Panel>
        </>
      )}
    </>
  )
}

function Change({ value, suffix = ' %' }: { value: number | null | undefined; suffix?: string }) {
  if (value === null || value === undefined) return <p className="stat-foot">Sin variación calculable</p>
  // For pollutants a decrease is an improvement.
  const better = value < 0
  const Icon = value < 0 ? ArrowDownRight : ArrowUpRight
  return (
    <p className={`stat-foot row ${value === 0 ? '' : better ? 'delta-good' : 'delta-bad'}`} style={{ gap: 4, fontWeight: 600 }}>
      <Icon size={14} />{suffix === ' %' ? signedPercent(value) : `${value > 0 ? '+' : ''}${value}${suffix}`}
      <span className="muted" style={{ fontWeight: 400 }}>{value === 0 ? 'sin cambio' : better ? 'mejora' : 'deterioro'}</span>
    </p>
  )
}

function DeltaCard({ label, a, b, change, unit }: { label: string; a: number | null; b: number | null; change: number | null | undefined; unit: string }) {
  return (
    <div className="panel kpi">
      <p className="stat-label">{label}</p>
      <p className="stat-value">{num(a)} → {num(b)}<span className="stat-unit">{unit}</span></p>
      <Change value={change} />
    </div>
  )
}
