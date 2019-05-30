/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import * as React from 'react'
import { bindActionCreators, Dispatch } from 'redux'
import { connect } from 'react-redux'

// Components
import Grant from './grant'
import AdsBox from './adsBox'
import ContributeBox from './contributeBox'
import DonationBox from './donationsBox'
import PageWallet from './pageWallet'
import {
  MainToggleMobile,
  SettingsPageMobile,
  WalletInfoHeader
} from 'brave-ui/src/features/rewards/mobile'
import { StyledDisabledContent, StyledHeading, StyledText } from './style'

// Utils
import * as utils from '../utils'
import { getLocale } from '../../../common/locale'
import * as rewardsActions from '../actions/rewards_actions'

interface State {
  mainToggle: boolean
  walletShown: boolean
}

export interface Props extends Rewards.ComponentProps {
  rewardsEnabled?: boolean
}

class SettingsPage extends React.Component<Props, State> {
  constructor (props: Props) {
    super(props)
    this.state = {
      mainToggle: true,
      walletShown: false
    }
  }

  get actions () {
    return this.props.actions
  }

  onToggle = () => {
    this.setState({ mainToggle: !this.state.mainToggle })
    this.actions.onSettingSave('enabledMain', !this.props.rewardsData.enabledMain)
  }

  onToggleWallet = () => {
    if (this.state.walletShown) {
      window.location.hash = ''
    } else {
      window.location.hash = '#rewards-summary'
    }

    this.setState({ walletShown: !this.state.walletShown })
  }

  componentDidMount () {
    if (this.props.rewardsData.firstLoad === null) {
      // First load ever
      this.actions.onSettingSave('firstLoad', true)
      this.actions.getWalletPassphrase()
    } else if (this.props.rewardsData.firstLoad) {
      // Second load ever
      this.actions.onSettingSave('firstLoad', false)
    }

    if (this.props.rewardsData.adsData.adsEnabled) {
      this.actions.getTransactionHistoryForThisCycle()
    }

    this.isWalletUrl()

    window.addEventListener('popstate', (e) => {
      this.isWalletUrl()
    })
    window.addEventListener('hashchange', (e) => {
      this.isWalletUrl()
    })
  }

  componentDidUpdate (prevProps: Props) {
    if (
      !prevProps.rewardsData.enabledMain &&
      this.props.rewardsData.enabledMain
    ) {
      this.actions.getContributeList()
      this.actions.getWalletProperties()
    }

    if (
      !prevProps.rewardsData.adsData.adsEnabled &&
      this.props.rewardsData.adsData.adsEnabled
    ) {
      this.actions.getTransactionHistoryForThisCycle()
    }

    if (
      !prevProps.rewardsData.enabledContribute &&
      this.props.rewardsData.enabledContribute
    ) {
      this.actions.getContributeList()
    }
  }

  isWalletUrl = () => {
    const walletShown = (
      window &&
      window.location &&
      window.location.hash &&
      window.location.hash === '#rewards-summary'
    )

    this.setState({
      walletShown: !!walletShown
    })
  }

  getGrantClaims = () => {
    const { grants } = this.props.rewardsData

    if (!grants) {
      return null
    }

    return (
      <>
        {grants.map((grant?: Rewards.Grant, index?: number) => {
          if (!grant || !grant.promotionId) {
            return null
          }

          return (
            <div key={`grant-${index}`}>
              <Grant grant={grant} />
            </div>
          )
        })}
      </>
    )
  }

  componentWillUnmount () {
    const { rewardsIntervalId } = this.props.rewardsData

    if (rewardsIntervalId) {
      window.clearInterval(rewardsIntervalId)
    }
  }

  render () {
    const { enabledMain } = this.props.rewardsData
    const { balance, rates } = this.props.rewardsData.walletInfo
    const convertedBalance = utils.convertBalance((balance || 0).toString(), rates)

    return (
      <SettingsPageMobile>
        <MainToggleMobile
          onToggle={this.onToggle}
          enabled={enabledMain}
        />
        {
          !this.state.mainToggle && !enabledMain
          ? <StyledDisabledContent>
              <StyledHeading>
                {getLocale('rewardsWhy')}
              </StyledHeading>
              <StyledText>
                {getLocale('whyBraveRewardsDesc1')}
              </StyledText>
              <StyledText>
                {getLocale('whyBraveRewardsDesc2')}
              </StyledText>
            </StyledDisabledContent>
          : null
        }
        {
          enabledMain
          ? this.getGrantClaims()
          : null
        }
        <WalletInfoHeader
          onClick={this.onToggleWallet}
          balance={balance.toString()}
          id={'mobile-wallet'}
          converted={`${convertedBalance} USD`}
        />
        <AdsBox />
        <ContributeBox />
        <DonationBox />
        <PageWallet
          toggleAction={this.onToggleWallet}
          visible={this.state.walletShown}
        />
      </SettingsPageMobile>
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
)(SettingsPage)
