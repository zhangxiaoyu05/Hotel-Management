import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import PriceBreakdown from '@/components/business/PriceBreakdown.vue'
import type { PriceBreakdown as PriceBreakdownType } from '@/types/order'

describe('PriceBreakdown.vue', () => {
  let wrapper: any

  const mockPriceBreakdown: PriceBreakdownType = {
    roomFee: 298,
    serviceFee: 30,
    discountAmount: 20,
    totalPrice: 308,
    nights: 1,
    roomRate: 298,
    couponCode: 'SAVE10',
    taxes: [
      { name: '服务税', amount: 15 },
      { name: '城市税', amount: 15 }
    ]
  }

  beforeEach(() => {
    wrapper = mount(PriceBreakdown, {
      props: {
        priceBreakdown: mockPriceBreakdown,
        loading: false
      }
    })
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it('renders price breakdown correctly', () => {
    expect(wrapper.find('.price-breakdown').exists()).toBe(true)
    expect(wrapper.find('.room-fee').text()).toContain('298.00')
    expect(wrapper.find('.service-fee').text()).toContain('30.00')
    expect(wrapper.find('.discount-amount').text()).toContain('-20.00')
    expect(wrapper.find('.total-price').text()).toContain('308.00')
  })

  it('displays correct number of nights', () => {
    expect(wrapper.text()).toContain(`共 ${mockPriceBreakdown.nights} 晚`)
  })

  it('shows coupon code when applied', () => {
    expect(wrapper.text()).toContain(`优惠码: ${mockPriceBreakdown.couponCode}`)
  })

  it('displays taxes correctly', () => {
    const taxItems = wrapper.findAll('.tax-item')
    expect(taxItems).toHaveLength(2)
    expect(taxItems[0].text()).toContain('服务税')
    expect(taxItems[0].text()).toContain('15.00')
    expect(taxItems[1].text()).toContain('城市税')
    expect(taxItems[1].text()).toContain('15.00')
  })

  it('formats currency correctly', () => {
    const priceElements = wrapper.findAll('.price-value')
    priceElements.forEach(el => {
      const text = el.text()
      // Should contain currency symbol and two decimal places
      expect(text).toMatch(/¥\d+\.\d{2}/)
    })
  })

  it('shows loading state correctly', async () => {
    await wrapper.setProps({ loading: true })

    expect(wrapper.find('.loading-skeleton').exists()).toBe(true)
    expect(wrapper.find('.price-details').exists()).toBe(false)
  })

  it('calculates per night rate correctly', () => {
    const ratePerNight = wrapper.find('.rate-per-night')
    const expectedRate = mockPriceBreakdown.roomFee / mockPriceBreakdown.nights
    expect(ratePerNight.text()).toContain(`${expectedRate.toFixed(2)}`)
  })

  it('handles zero discount correctly', async () => {
    const noDiscountBreakdown = {
      ...mockPriceBreakdown,
      discountAmount: 0,
      couponCode: undefined
    }

    await wrapper.setProps({ priceBreakdown: noDiscountBreakdown })

    expect(wrapper.find('.discount-section').exists()).toBe(false)
    expect(wrapper.find('.coupon-applied').exists()).toBe(false)
  })

  it('shows savings when discount is applied', () => {
    const savingsElement = wrapper.find('.savings-amount')
    expect(savingsElement.exists()).toBe(true)
    expect(savingsElement.text()).toContain(`节省 ¥${mockPriceBreakdown.discountAmount.toFixed(2)}`)
  })

  it('displays breakdown details on expand', async () => {
    const toggleButton = wrapper.find('[data-test="toggle-details"]')

    // Initially collapsed
    expect(wrapper.find('.detailed-breakdown').exists()).toBe(false)

    // Expand details
    await toggleButton.trigger('click')

    expect(wrapper.find('.detailed-breakdown').exists()).toBe(true)
    expect(wrapper.find('.tax-breakdown').exists()).toBe(true)
  })

  it('emits update event when coupon is applied', async () => {
    const couponInput = wrapper.find('[data-test="coupon-input"]')
    const applyButton = wrapper.find('[data-test="apply-coupon"]')

    await couponInput.setValue('NEWCOUPON')
    await applyButton.trigger('click')

    expect(wrapper.emitted('apply-coupon')).toBeTruthy()
    expect(wrapper.emitted('apply-coupon')[0]).toEqual(['NEWCOUPON'])
  })

  it('validates coupon code format', async () => {
    const couponInput = wrapper.find('[data-test="coupon-input"]')
    const applyButton = wrapper.find('[data-test="apply-coupon"]')

    // Test invalid coupon (too short)
    await couponInput.setValue('AB')
    await applyButton.trigger('click')

    expect(wrapper.text()).toContain('优惠码格式不正确')
  })

  it('handles multiple nights correctly', async () => {
    const multiNightBreakdown = {
      ...mockPriceBreakdown,
      roomFee: 596,
      nights: 2,
      totalPrice: 606
    }

    await wrapper.setProps({ priceBreakdown: multiNightBreakdown })

    expect(wrapper.text()).toContain('共 2 晚')
    expect(wrapper.find('.room-fee').text()).toContain('596.00')
    const ratePerNight = wrapper.find('.rate-per-night')
    const expectedRate = multiNightBreakdown.roomFee / multiNightBreakdown.nights
    expect(ratePerNight.text()).toContain(`${expectedRate.toFixed(2)}`)
  })

  it('applies promotional discount correctly', async () => {
    const promoBreakdown = {
      ...mockPriceBreakdown,
      discountAmount: 50,
      promotionLabel: '早鸟特惠'
    }

    await wrapper.setProps({ priceBreakdown: promoBreakdown })

    expect(wrapper.text()).toContain(promoBreakdown.promotionLabel)
    expect(wrapper.find('.discount-amount').text()).toContain('-50.00')
  })

  it('shows price change animation when updated', async () => {
    const newBreakdown = {
      ...mockPriceBreakdown,
      totalPrice: 280
    }

    await wrapper.setProps({ priceBreakdown: newBreakdown })

    const totalPriceElement = wrapper.find('.total-price')
    expect(totalPriceElement.classes()).toContain('price-changed')
  })
})