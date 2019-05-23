/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import * as React from 'react'
import { bindActionCreators, Dispatch } from 'redux'
import { connect } from 'react-redux'

// Components
import { BoxMobile } from 'brave-ui/src/features/rewards/mobile'
import { List, NextContribution, Tokens } from 'brave-ui/src/features/rewards'
import { Column, Grid, Select, ControlWrapper } from 'brave-ui/src/components'
import {
  StyledListContent,
  StyledTotalContent
} from './style'

// Utils
import { getLocale } from '../../../common/locale'
import * as rewardsActions from '../actions/rewards_actions'
import * as utils from '../utils'

interface Props extends Rewards.ComponentProps {
}

class AdsBox extends React.Component<Props, {}> {
  constructor (props: Props) {
    super(props)
  }

  onAdsSettingChange = (key: string, value: string) => {
    let newValue: any = value
    const { adsEnabled } = this.props.rewardsData.adsData

    if (key === 'adsEnabled') {
      newValue = !adsEnabled
    }

    this.props.actions.onAdsSettingSave(key, newValue)
  }

  adsSettings = (enabled?: boolean) => {
    if (!enabled) {
      return null
    }

    const { adsPerHour } = this.props.rewardsData.adsData

    return (
      <Grid columns={1} customStyle={{ maxWidth: '270px', margin: '0 auto' }}>
        <Column size={1} customStyle={{ justifyContent: 'center', flexWrap: 'wrap' }}>
          <ControlWrapper text={getLocale('adsPerHour')}>
            <Select
              value={adsPerHour.toString()}
              onChange={this.onAdsSettingChange.bind(this, 'adsPerHour')}
            >
              {['1', '2', '3', '4', '5'].map((num: string) => {
                return (
                  <div key={`num-per-hour-${num}`} data-value={num}>
                   {getLocale(`adsPerHour${num}`)}
                  </div>
                )
              })}
            </Select>
          </ControlWrapper>
        </Column>
      </Grid>
    )
  }

  getBoxMessage = (enabled?: boolean) => {
    if (enabled) {
      return getLocale('adsDesc')
    }

    return getLocale('adsDisabledText')
  }

  render () {
    // Default values from storage.ts
    let adsEnabled = false
    let adsUIEnabled = false
    let notificationsReceived = 0
    let estimatedEarnings = '0'
    // let adsIsSupported = false
    const {
      adsData,
      enabledMain,
      walletInfo
    } = this.props.rewardsData

    if (adsData) {
      adsEnabled = adsData.adsEnabled
      adsUIEnabled = adsData.adsUIEnabled
      notificationsReceived = adsData.adsNotificationsReceived || 0
      estimatedEarnings = (adsData.adsEstimatedEarnings || 0).toFixed(2)
      // adsIsSupported = adsData.adsIsSupported
    }

    const toggle = !(!enabledMain || !adsUIEnabled)

    return (
      <BoxMobile
        title={getLocale('adsTitle')}
        type={'ads'}
        description={this.getBoxMessage(toggle)}
        settingsChild={this.adsSettings(adsEnabled && enabledMain)}
        toggle={toggle}
        checked={enabledMain && adsEnabled}
        toggleAction={this.onAdsSettingChange.bind(this, 'adsEnabled', '')}
      >
        <List title={<StyledListContent>{getLocale('adsCurrentEarnings')}</StyledListContent>}>
          <StyledTotalContent>
            <Tokens
              value={estimatedEarnings}
              converted={utils.convertBalance(estimatedEarnings, walletInfo.rates)}
            />
          </StyledTotalContent>
        </List>
        <List title={<StyledListContent>{getLocale('adsPaymentDate')}</StyledListContent>}>
          <StyledListContent>
            <NextContribution>
              {'Monthly, 5th'}
            </NextContribution>
          </StyledListContent>
        </List>
        <List title={<StyledListContent>{getLocale('adsNotificationsReceived')}</StyledListContent>}>
          <StyledListContent>
            <Tokens
              value={notificationsReceived.toString()}
              hideText={true}
            />
          </StyledListContent>
        </List>
      </BoxMobile>
    )
  }
}

const mapStateToProps = (state: Rewards.ApplicationState) => ({
  rewardsData: state.rewardsData
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  actions: bindActionCreators(rewardsActions, dispatch)
})

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AdsBox)
