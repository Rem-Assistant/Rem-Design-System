// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2190-12237
// source=compose/RemDesignSystem/agentsurfaces/DailyBriefCard.kt
// component=DailyBriefCard
import figma from 'figma'
const instance = figma.selectedInstance
// Every canonical value is handled. Finished/Retry have no representation in isReading.
const playback = instance.getEnum('Playback', {
    Ready: 'Ready', Reading: 'Reading', Finished: 'Finished', Retry: 'Retry',
})
const quote = (value: string) => JSON.stringify(value).replace(/\$/g, '\\$')
const title = quote(instance.getString('Headline'))
const summary = quote(instance.getString('Summary'))
const isReading = playback === 'Reading'
const supported = playback === 'Ready' || playback === 'Reading'
export default {
    example: supported
        ? figma.code`DailyBriefCard(title = ${title}, summary = ${summary}, onTap = onTap, onRead = onRead, isReading = ${isReading})`
        : figma.code`// Unsupported DailyBriefCard Playback: ${playback}.
// The shipped API only has isReading. Finished/Retry require a separately approved state API.
// No substitute Ready state is emitted.`,
    imports: ['import com.rem.designsystem.agentsurfaces.DailyBriefCard'],
    id: 'rem-daily-brief-card-compose',
    metadata: { nestable: false, props: { playback, supported } },
}
