import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import { Row, BackButton } from '@platform-ui/pageHeader';

interface Props {
  title: string;
  backText?: string;
  backPath?: string;
  right?: React.ReactNode;
}

export default function PageHeader(props: Props) {
  const { title, backText, backPath = '', right } = props;
  const navigate = useNavigate(backPath);
  const backButton = backText && backPath && (
    <BackButton onClick={navigate}>{backText}</BackButton>
  );
  return <Row title={title} right={right} backButton={backButton} />;
}
